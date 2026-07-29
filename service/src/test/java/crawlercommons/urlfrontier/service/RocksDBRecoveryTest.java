// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import crawlercommons.urlfrontier.Urlfrontier;
import crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams;
import crawlercommons.urlfrontier.service.rocksdb.RocksDBService;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

class RocksDBRecoveryTest {

    private static final String ROCKSDB_PATH = "./target/rocksdb-recovery";

    private RocksDBService service;

    @BeforeEach
    void setup() {
        FileUtils.deleteQuietly(new File(ROCKSDB_PATH));
    }

    /** Releases the lock on the directory even if a test failed halfway through * */
    @AfterEach
    void cleanup() throws IOException {
        stop();
        FileUtils.deleteQuietly(new File(ROCKSDB_PATH));
    }

    /** Closes the current instance if there is one and opens a new one on the same directory * */
    private RocksDBService restart() throws IOException {
        return restart(false);
    }

    private RocksDBService restart(boolean checkOnRecovery) throws IOException {
        return restart(
                checkOnRecovery
                        ? Map.of("rocksdb.recovery.check", "true")
                        : Map.<String, String>of());
    }

    private RocksDBService restart(Map<String, String> extraConf) throws IOException {
        stop();
        Map<String, String> conf = new HashMap<>(extraConf);
        conf.put("rocksdb.path", ROCKSDB_PATH);
        service = new RocksDBService(conf, "localhost", 7071);
        return service;
    }

    private void stop() throws IOException {
        if (service != null) {
            RocksDBService toClose = service;
            // don't try closing it twice if it fails
            service = null;
            toClose.close();
        }
    }

    /**
     * The queue infos table must be emptied once it has been read, otherwise a leftover entry makes
     * the next restart believe that it holds the full list of queues - see #163
     */
    @Test
    void queueInfosAreFullyClearedOnStartup() throws IOException {

        // the queue infos get persisted on a clean shutdown
        RocksDBService frontier = restart();
        ServiceTestUtil.initURLs(frontier);
        assertEquals(3, frontier.getQueues().size());

        // restart: the queue infos are read and the table must be emptied
        frontier = restart();
        assertEquals(3, frontier.getQueues().size());

        // get rid of every queue: the queue infos table must not
        // contain anything about them anymore
        for (QueueWithinCrawl qwc : new ArrayList<>(frontier.getQueues().keySet())) {
            deleteQueue(frontier, qwc);
        }
        assertEquals(0, frontier.getQueues().size());

        // any entry left over from the previous restart would be resurrected here
        frontier = restart();
        assertEquals(0, frontier.getQueues().size());
    }

    /**
     * With the WAL disabled, a clean shutdown flushes the memtables: everything written must
     * survive a restart. The recovery check on the second start-up scans the URL tables themselves,
     * so it fails if their content was lost with the memtables.
     */
    @Test
    void urlsSurviveCleanRestartWithoutWAL() throws IOException {

        RocksDBService frontier = restart(Map.of("rocksdb.wal.disable", "true"));
        ServiceTestUtil.initURLs(frontier);
        final Set<QueueWithinCrawl> expected = new HashSet<>(frontier.getQueues().keySet());
        assertEquals(3, expected.size());

        frontier =
                restart(
                        Map.of(
                                "rocksdb.wal.disable", "true",
                                "rocksdb.recovery.check", "true"));
        assertEquals(expected, new HashSet<>(frontier.getQueues().keySet()));
    }

    /** The queues must all be recovered when the previous instance did not shut down cleanly * */
    @Test
    void allQueuesRecoveredAfterCrash() throws IOException {

        RocksDBService frontier = restart();
        ServiceTestUtil.initURLs(frontier);
        final Set<QueueWithinCrawl> expected = new HashSet<>(frontier.getQueues().keySet());
        assertEquals(3, expected.size());

        // this instance reads the queue infos and empties the table but,
        // as it does not get closed properly, does not write them back
        frontier = restart();
        assertEquals(3, frontier.getQueues().size());
        // simulates an unclean shutdown: the RocksDB instance is released so that
        // it can be reopened but the stats about the queues are not persisted
        frontier.getQueues().clear();

        // the queues have to be rebuilt from the URL tables
        frontier = restart();
        assertEquals(expected, new HashSet<>(frontier.getQueues().keySet()));
    }

    /**
     * Deleting a queue must remove everything it owns from all the tables, including for the last
     * queue in lexicographic order, otherwise the queues get resurrected on the next start-up
     */
    @Test
    void deletedQueuesAreNotResurrected() throws IOException {

        RocksDBService frontier = restart();
        ServiceTestUtil.initURLs(frontier);
        assertEquals(3, frontier.getQueues().size());

        for (QueueWithinCrawl qwc : new ArrayList<>(frontier.getQueues().keySet())) {
            deleteQueue(frontier, qwc);
        }
        assertEquals(0, frontier.getQueues().size());

        // the queues get rebuilt from the scheduling table: whatever the deletions
        // left behind comes back as a queue
        frontier = restart(true);
        assertEquals(0, frontier.getQueues().size());
    }

    /**
     * The rows left behind by a deletion are not necessarily visible through the API - check the
     * tables directly
     */
    @Test
    void deletedQueuesLeaveNothingBehind() throws IOException, RocksDBException {

        RocksDBService frontier = restart();
        ServiceTestUtil.initURLs(frontier);
        assertEquals(3, frontier.getQueues().size());

        for (QueueWithinCrawl qwc : new ArrayList<>(frontier.getQueues().keySet())) {
            deleteQueue(frontier, qwc);
        }

        // release the lock on the directory so that the tables can be read
        stop();

        assertEquals(List.of(), remainingKeys("default"));
        assertEquals(List.of(), remainingKeys("queues"));
        assertEquals(List.of(), remainingKeys("creationDates"));
    }

    /** Lists the keys left in one of the column families used by the service * */
    private List<String> remainingKeys(String columnFamily) throws RocksDBException {

        final List<String> keys = new ArrayList<>();

        try (ColumnFamilyOptions cfOpts = new ColumnFamilyOptions();
                DBOptions options = new DBOptions()) {

            final List<ColumnFamilyDescriptor> cfDescriptors =
                    Arrays.asList(
                            new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts),
                            new ColumnFamilyDescriptor("queues".getBytes(), cfOpts),
                            new ColumnFamilyDescriptor("queueInfos".getBytes(), cfOpts),
                            new ColumnFamilyDescriptor("creationDates".getBytes(), cfOpts));

            final List<ColumnFamilyHandle> handles = new ArrayList<>();

            try (RocksDB db = RocksDB.open(options, ROCKSDB_PATH, cfDescriptors, handles)) {
                try {
                    int index = 0;
                    for (int i = 0; i < cfDescriptors.size(); i++) {
                        if (new String(cfDescriptors.get(i).getName()).equals(columnFamily)) {
                            index = i;
                        }
                    }
                    try (RocksIterator iter = db.newIterator(handles.get(index))) {
                        for (iter.seekToFirst(); iter.isValid(); iter.next()) {
                            keys.add(new String(iter.key()));
                        }
                    }
                } finally {
                    for (ColumnFamilyHandle handle : handles) {
                        handle.close();
                    }
                }
            }
        }

        return keys;
    }

    private void deleteQueue(RocksDBService service, QueueWithinCrawl qwc) {

        StreamObserver<Urlfrontier.Long> observer =
                new StreamObserver<>() {

                    @Override
                    public void onNext(Urlfrontier.Long value) {}

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        fail();
                    }

                    @Override
                    public void onCompleted() {}
                };

        service.deleteQueue(
                QueueWithinCrawlParams.newBuilder()
                        .setKey(qwc.getQueue())
                        .setCrawlID(qwc.getCrawlid())
                        .build(),
                observer);
    }
}
