// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
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
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** A sharded service configured with a single node must behave exactly like before the fix. */
class SingleNodeShardedServiceTest {

    private static final int PORT = 7401;
    private static final String PATH = "./target/rocksdb-single";

    static ShardedRocksDBService service;
    static Server server;
    static ManagedChannel channel;
    static URLFrontierBlockingStub stub;

    @BeforeAll
    static void setup() throws IOException {
        FileUtils.deleteQuietly(new File(PATH));
        Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.path", PATH);
        conf.put("nodes", "localhost:" + PORT);
        service = new ShardedRocksDBService(conf, "localhost", PORT);
        server = ServerBuilder.forPort(PORT).addService(service).build().start();
        channel = ManagedChannelBuilder.forTarget("localhost:" + PORT).usePlaintext().build();
        stub = URLFrontierGrpc.newBlockingStub(channel);
    }

    @AfterAll
    static void teardown() throws Exception {
        if (channel != null) {
            try {
                channel.shutdownNow();
                channel.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (server != null && !server.isTerminated()) {
            try {
                server.shutdownNow();
                server.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            if (service != null) {
                service.close();
            }
        } finally {
            FileUtils.deleteQuietly(new File(PATH));
        }
    }

    @Test
    void keyedSetDelayLocalFalseAppliedLocally() throws Exception {
        assertFalse(service.clusterMode, "single-node config must not enable cluster mode");
        String key = "single.test";
        CountDownLatch latch = new CountDownLatch(1);
        StreamObserver<URLItem> input =
                URLFrontierGrpc.newStub(channel)
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
                        .setCrawlID("DEFAULT")
                        .build();
        input.onNext(
                URLItem.newBuilder()
                        .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                        .build());
        input.onCompleted();
        assertTrue(latch.await(10, TimeUnit.SECONDS), "putURLs did not complete in time");

        stub.setDelay(
                QueueDelayParams.newBuilder()
                        .setKey(key)
                        .setCrawlID("DEFAULT")
                        .setDelayRequestable(66)
                        .setLocal(false)
                        .build());

        assertEquals(66, service.getQueues().get(QueueWithinCrawl.get(key, "DEFAULT")).getDelay());
    }
}
