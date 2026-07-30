// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.Urlfrontier.AckMessage.Status;
import crawlercommons.urlfrontier.Urlfrontier.BatchAck;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredBatch;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.service.rocksdb.RocksDBService;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Exercises the batched ingestion of discovered URLs on the RocksDB implementation */
class PutDiscoveredTest {

    private static final String ROCKSDB_PATH = "./target/rocksdb-putdiscovered";

    private RocksDBService frontier;

    @BeforeEach
    void setup() {
        FileUtils.deleteQuietly(new File(ROCKSDB_PATH));
        Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.path", ROCKSDB_PATH);
        frontier = new RocksDBService(conf, "localhost", 7071);
    }

    @AfterEach
    void teardown() throws IOException {
        frontier.close();
        FileUtils.deleteQuietly(new File(ROCKSDB_PATH));
    }

    private static URLInfo info(String url) {
        return URLInfo.newBuilder().setUrl(url).build();
    }

    /** Sends one batch and returns its ack once the stream has completed */
    private BatchAck send(String id, List<URLInfo> items) throws InterruptedException {

        final List<BatchAck> acks = new ArrayList<>();
        final CountDownLatch finished = new CountDownLatch(1);

        StreamObserver<DiscoveredBatch> stream =
                frontier.putDiscovered(
                        new StreamObserver<>() {
                            @Override
                            public void onNext(BatchAck value) {
                                acks.add(value);
                            }

                            @Override
                            public void onError(Throwable t) {
                                finished.countDown();
                            }

                            @Override
                            public void onCompleted() {
                                finished.countDown();
                            }
                        });

        stream.onNext(DiscoveredBatch.newBuilder().setID(id).addAllItems(items).build());
        stream.onCompleted();

        assertTrue(finished.await(30, TimeUnit.SECONDS), "stream did not complete");
        assertEquals(1, acks.size());
        return acks.get(0);
    }

    @Test
    void newURLsAreCreated() throws Exception {
        BatchAck ack =
                send(
                        "b1",
                        List.of(
                                info("http://a.com/1"),
                                info("http://b.com/1"),
                                info("http://a.com/2")));

        assertEquals("b1", ack.getID());
        assertEquals(List.of(Status.OK, Status.OK, Status.OK), ack.getStatusesList());
        assertEquals(2, frontier.getQueues().size());

        int active = 0;
        for (QueueInterface q : frontier.getQueues().values()) {
            active += q.countActive();
        }
        assertEquals(3, active);
    }

    @Test
    void duplicateWithinTheBatchIsSkipped() throws Exception {
        BatchAck ack = send("b1", List.of(info("http://a.com/1"), info("http://a.com/1")));

        assertEquals(List.of(Status.OK, Status.SKIPPED), ack.getStatusesList());
        assertEquals(1, frontier.getQueues().values().iterator().next().countActive());
    }

    @Test
    void knownURLIsSkipped() throws Exception {
        assertEquals(
                List.of(Status.OK), send("b1", List.of(info("http://a.com/1"))).getStatusesList());
        assertEquals(
                List.of(Status.SKIPPED),
                send("b2", List.of(info("http://a.com/1"))).getStatusesList());
        assertEquals(1, frontier.getQueues().values().iterator().next().countActive());
    }

    @Test
    void malformedURLIsSkippedOthersGoThrough() throws Exception {
        // no hostname can be derived from an empty URL
        BatchAck ack = send("b1", List.of(info(""), info("http://a.com/1")));
        assertEquals(List.of(Status.SKIPPED, Status.OK), ack.getStatusesList());
    }

    /** An empty batch works and echoes the ID: clients use it to probe for support */
    @Test
    void emptyBatchIsAcked() throws Exception {
        BatchAck ack = send("probe", List.of());
        assertEquals("probe", ack.getID());
        assertEquals(0, ack.getStatusesCount());
    }
}
