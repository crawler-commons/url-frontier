// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.BlockQueueParams;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import crawlercommons.urlfrontier.service.rocksdb.ShardedRocksDBService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Two sharded RocksDB services behind real gRPC servers. Tests are ordered: routing behaviour
 * first, node-failure behaviour last (node B is shut down and stays down).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DistributedFrontierServiceTest {

    private static final int PORT_A = 7301;
    private static final int PORT_B = 7302;
    private static final List<String> NODES = List.of("localhost:" + PORT_A, "localhost:" + PORT_B);
    private static final String PATH_A = "./target/rocksdb-shard-a";
    private static final String PATH_B = "./target/rocksdb-shard-b";

    static ShardedRocksDBService serviceA;
    static ShardedRocksDBService serviceB;
    static Server serverA;
    static Server serverB;
    static ManagedChannel channelA;
    static URLFrontierBlockingStub stubA;

    @BeforeAll
    static void setup() throws IOException {
        FileUtils.deleteQuietly(new File(PATH_A));
        FileUtils.deleteQuietly(new File(PATH_B));
        serviceA = newService(PATH_A, PORT_A);
        serviceB = newService(PATH_B, PORT_B);
        serverA = ServerBuilder.forPort(PORT_A).addService(serviceA).build().start();
        serverB = ServerBuilder.forPort(PORT_B).addService(serviceB).build().start();
        channelA = ManagedChannelBuilder.forTarget("localhost:" + PORT_A).usePlaintext().build();
        stubA = URLFrontierGrpc.newBlockingStub(channelA);
    }

    private static ShardedRocksDBService newService(String path, int port) {
        Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.path", path);
        conf.put("nodes", String.join(",", NODES));
        return new ShardedRocksDBService(conf, "localhost", port);
    }

    @AfterAll
    static void teardown() throws Exception {
        if (channelA != null) {
            try {
                channelA.shutdownNow();
                channelA.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        for (Server server : new Server[] {serverA, serverB}) {
            if (server == null || server.isTerminated()) {
                continue;
            }
            try {
                server.shutdownNow();
                server.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            if (serviceA != null) {
                serviceA.close();
            }
        } finally {
            try {
                if (serviceB != null) {
                    serviceB.close();
                }
            } finally {
                FileUtils.deleteQuietly(new File(PATH_A));
                FileUtils.deleteQuietly(new File(PATH_B));
            }
        }
    }

    /** Finds a key owned by the wanted partition, distinct per label. */
    private static String keyOwnedBy(int partition, String label) {
        for (int i = 0; i < 10_000; i++) {
            String key = label + "-" + i + ".test";
            if (DistributedFrontierService.partitionFor(QueueWithinCrawl.get(key, "DEFAULT"), NODES)
                    == partition) {
                return key;
            }
        }
        throw new IllegalStateException("no key found for partition " + partition);
    }

    /**
     * Creates a queue by sending a URL through node A's gRPC putURLs endpoint; the distributed
     * service routes it to the owner. Never call service.putURLs directly: it casts the observer to
     * ServerCallStreamObserver.
     */
    private static void createQueue(String key, String crawlId) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<URLItem> input =
                URLFrontierGrpc.newStub(channelA)
                        .putURLs(
                                new StreamObserver<AckMessage>() {
                                    @Override
                                    public void onNext(AckMessage value) {}

                                    @Override
                                    public void onError(Throwable t) {
                                        latch.countDown();
                                    }

                                    @Override
                                    public void onCompleted() {
                                        latch.countDown();
                                    }
                                });
        URLInfo info =
                URLInfo.newBuilder()
                        .setUrl("https://" + key + "/page")
                        .setKey(key)
                        .setCrawlID(crawlId)
                        .build();
        input.onNext(
                URLItem.newBuilder()
                        .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                        .build());
        input.onCompleted();
        assertTrue(latch.await(10, TimeUnit.SECONDS), "putURLs did not complete in time");
    }

    @Test
    @Order(1)
    void smokePutUrlsCreatesQueueOnOwnerOnly() throws Exception {
        String key = keyOwnedBy(1, "smoke");
        createQueue(key, "DEFAULT");
        QueueWithinCrawl qwc = QueueWithinCrawl.get(key, "DEFAULT");
        assertNotNull(serviceB.getQueues().get(qwc), "queue must exist on the owner (node B)");
        assertNull(serviceA.getQueues().get(qwc), "queue must not exist on the non-owner");
    }

    @Test
    @Order(2)
    void keyedSetDelayLocalFalseReachesOwner() throws Exception {
        String key = keyOwnedBy(1, "delay");
        createQueue(key, "DEFAULT");
        QueueWithinCrawl qwc = QueueWithinCrawl.get(key, "DEFAULT");

        // sent to the NON-owner node A with local=false
        stubA.setDelay(
                QueueDelayParams.newBuilder()
                        .setKey(key)
                        .setCrawlID("DEFAULT")
                        .setDelayRequestable(123)
                        .setLocal(false)
                        .build());

        assertEquals(
                123,
                serviceB.getQueues().get(qwc).getDelay(),
                "owner queue must receive the delay (silent no-op on master)");
        assertNull(serviceA.getQueues().get(qwc), "no queue must be created on the non-owner");
    }

    @Test
    @Order(3)
    void keylessSetDelayLocalFalseSetsDefaultOnEveryNode() {
        stubA.setDelay(
                QueueDelayParams.newBuilder().setDelayRequestable(77).setLocal(false).build());

        assertEquals(77, serviceA.getDefaultDelayForQueues());
        assertEquals(77, serviceB.getDefaultDelayForQueues());
    }

    @Test
    @Order(4)
    void keyedSetDelayLocalTrueNeverLeavesTheNode() throws Exception {
        String key = keyOwnedBy(1, "delaylocal");
        createQueue(key, "DEFAULT");
        QueueWithinCrawl qwc = QueueWithinCrawl.get(key, "DEFAULT");

        stubA.setDelay(
                QueueDelayParams.newBuilder()
                        .setKey(key)
                        .setCrawlID("DEFAULT")
                        .setDelayRequestable(555)
                        .setLocal(true)
                        .build());

        assertNotEquals(
                555,
                serviceB.getQueues().get(qwc).getDelay(),
                "local=true on the non-owner must not touch the owner queue");
    }

    @Test
    @Order(5)
    void keyedBlockQueueUntilLocalFalseReachesOwner() throws Exception {
        String key = keyOwnedBy(1, "block");
        createQueue(key, "DEFAULT");
        QueueWithinCrawl qwc = QueueWithinCrawl.get(key, "DEFAULT");
        long until = Instant.now().getEpochSecond() + 3600;

        stubA.blockQueueUntil(
                BlockQueueParams.newBuilder()
                        .setKey(key)
                        .setCrawlID("DEFAULT")
                        .setTime(until)
                        .setLocal(false)
                        .build());

        assertEquals(
                until,
                serviceB.getQueues().get(qwc).getBlockedUntil(),
                "owner queue must be blocked (silent no-op on master)");
    }

    @Test
    @Order(6)
    void emptyKeyBlockQueueUntilIsLocalNoOp() {
        // must not compute an artificial owner nor fail: historical local no-op preserved
        assertDoesNotThrow(
                () ->
                        stubA.blockQueueUntil(
                                BlockQueueParams.newBuilder()
                                        .setTime(9_999_999_999L)
                                        .setLocal(false)
                                        .build()));
        assertNull(
                serviceA.getQueues().get(QueueWithinCrawl.get("", "DEFAULT")),
                "no artificial queue must be created anywhere");
        assertNull(serviceB.getQueues().get(QueueWithinCrawl.get("", "DEFAULT")));
    }

    @Test
    @Order(7)
    void emptyCrawlIdIsNormalisedAcrossRouting() throws Exception {
        // QueueWithinCrawl.get normalises "" to DEFAULT, so the partition matches keyOwnedBy
        String key = keyOwnedBy(1, "normalise");
        createQueue(key, "");
        QueueWithinCrawl qwc = QueueWithinCrawl.get(key, "");

        stubA.setDelay(
                QueueDelayParams.newBuilder()
                        .setKey(key)
                        .setCrawlID("")
                        .setDelayRequestable(42)
                        .setLocal(false)
                        .build());

        assertEquals(42, serviceB.getQueues().get(qwc).getDelay());
    }

    @Test
    @Order(8)
    void queueAbsentOnOwnerIsSilentSuccess() {
        String key = keyOwnedBy(1, "absent");
        // no createQueue on purpose: same success/no-op as AbstractFrontierService today
        stubA.setDelay(
                QueueDelayParams.newBuilder()
                        .setKey(key)
                        .setCrawlID("DEFAULT")
                        .setDelayRequestable(55)
                        .setLocal(false)
                        .build());
        assertNull(serviceB.getQueues().get(QueueWithinCrawl.get(key, "DEFAULT")));
    }

    @Test
    @Order(9)
    void repeatedAssignmentIsIdempotent() throws Exception {
        String key = keyOwnedBy(1, "idem");
        createQueue(key, "DEFAULT");
        QueueWithinCrawl qwc = QueueWithinCrawl.get(key, "DEFAULT");
        QueueDelayParams params =
                QueueDelayParams.newBuilder()
                        .setKey(key)
                        .setCrawlID("DEFAULT")
                        .setDelayRequestable(88)
                        .setLocal(false)
                        .build();

        stubA.setDelay(params);
        stubA.setDelay(params);

        assertEquals(88, serviceB.getQueues().get(qwc).getDelay());
    }

    @Test
    @Order(20)
    void ownerUnreachableSurfacesError() throws Exception {
        serverB.shutdownNow();
        serverB.awaitTermination(5, TimeUnit.SECONDS);

        String key = keyOwnedBy(1, "down");
        assertThrows(
                StatusRuntimeException.class,
                () ->
                        stubA.withDeadlineAfter(
                                        DistributedFrontierService.FORWARD_DEADLINE_SECONDS + 10,
                                        TimeUnit.SECONDS)
                                .setDelay(
                                        QueueDelayParams.newBuilder()
                                                .setKey(key)
                                                .setCrawlID("DEFAULT")
                                                .setDelayRequestable(11)
                                                .setLocal(false)
                                                .build()),
                "forward to a dead owner must fail, not silently no-op");
    }

    @Test
    @Order(21)
    void partialBroadcastFailureSurfacesErrorButLocalApplies() {
        assertThrows(
                StatusRuntimeException.class,
                () ->
                        stubA.setDelay(
                                QueueDelayParams.newBuilder()
                                        .setDelayRequestable(99)
                                        .setLocal(false)
                                        .build()));
        // documented partial application: the reachable node applied the value
        assertEquals(99, serviceA.getDefaultDelayForQueues());
    }

    @Test
    @Order(22)
    void localTrueUnaffectedByDeadNode() throws Exception {
        String key = keyOwnedBy(0, "localalive");
        createQueue(key, "DEFAULT");
        QueueWithinCrawl qwc = QueueWithinCrawl.get(key, "DEFAULT");

        stubA.setDelay(
                QueueDelayParams.newBuilder()
                        .setKey(key)
                        .setCrawlID("DEFAULT")
                        .setDelayRequestable(33)
                        .setLocal(true)
                        .build());

        assertEquals(33, serviceA.getQueues().get(qwc).getDelay());
    }

    @Test
    @Order(23)
    void keyedLocalFalseOwnedLocallyWorksWithDeadNode() throws Exception {
        String key = keyOwnedBy(0, "ownerlocal");
        createQueue(key, "DEFAULT");
        QueueWithinCrawl qwc = QueueWithinCrawl.get(key, "DEFAULT");

        stubA.setDelay(
                QueueDelayParams.newBuilder()
                        .setKey(key)
                        .setCrawlID("DEFAULT")
                        .setDelayRequestable(44)
                        .setLocal(false)
                        .build());

        assertEquals(44, serviceA.getQueues().get(qwc).getDelay());
    }
}
