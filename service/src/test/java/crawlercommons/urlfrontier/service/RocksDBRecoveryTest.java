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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        if (service != null) {
            service.close();
            service = null;
        }
        FileUtils.deleteQuietly(new File(ROCKSDB_PATH));
    }

    /** Closes the current instance if there is one and opens a new one on the same directory * */
    private RocksDBService restart() throws IOException {
        if (service != null) {
            service.close();
        }
        Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.path", ROCKSDB_PATH);
        service = new RocksDBService(conf, "localhost", 7071);
        return service;
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
