// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import crawlercommons.urlfrontier.service.rocksdb.ShardedRocksDBService;
import io.grpc.ForwardingServerCall;
import io.grpc.ForwardingServerCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Lifecycle of the per-partition putURLs streams node A opens onto node B (issue #207): one stream
 * is shared by every client and kept open, and dropping it from the cache closes the call instead
 * of leaving it half-open on both nodes.
 */
class ForwardingStreamLifecycleTest {

    private static final int PORT_A = 7311;
    private static final int PORT_B = 7312;
    private static final List<String> NODES = List.of("localhost:" + PORT_A, "localhost:" + PORT_B);
    private static final String PATH_A = "./target/rocksdb-forwarding-a";
    private static final String PATH_B = "./target/rocksdb-forwarding-b";

    /** Counts the putURLs calls node B serves, and how they end. */
    private static final AtomicInteger putURLsStarted = new AtomicInteger();

    private static final AtomicInteger putURLsClosed = new AtomicInteger();
    private static final List<Status.Code> closeCodes =
            Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger halfClosed = new AtomicInteger();

    // typed as the base class: discardForwardingStream is package-private to this package
    static DistributedFrontierService serviceA;

    static ShardedRocksDBService serviceB;
    static Server serverA;
    static Server serverB;
    static ManagedChannel channelA;

    private static final ServerInterceptor counter =
            new ServerInterceptor() {
                @Override
                public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                        ServerCall<ReqT, RespT> call,
                        Metadata headers,
                        ServerCallHandler<ReqT, RespT> next) {
                    if (!call.getMethodDescriptor()
                            .getFullMethodName()
                            .endsWith("URLFrontier/PutURLs")) {
                        return next.startCall(call, headers);
                    }
                    putURLsStarted.incrementAndGet();
                    ServerCall<ReqT, RespT> counted =
                            new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                                @Override
                                public void close(Status status, Metadata trailers) {
                                    closeCodes.add(status.getCode());
                                    putURLsClosed.incrementAndGet();
                                    super.close(status, trailers);
                                }
                            };
                    return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(
                            next.startCall(counted, headers)) {
                        @Override
                        public void onHalfClose() {
                            halfClosed.incrementAndGet();
                            super.onHalfClose();
                        }

                        @Override
                        public void onCancel() {
                            putURLsClosed.incrementAndGet();
                            closeCodes.add(Status.Code.CANCELLED);
                            super.onCancel();
                        }
                    };
                }
            };

    @BeforeAll
    static void setup() throws IOException {
        FileUtils.deleteQuietly(new File(PATH_A));
        FileUtils.deleteQuietly(new File(PATH_B));
        serviceA = newService(PATH_A, PORT_A);
        serviceB = newService(PATH_B, PORT_B);
        serverA = ServerBuilder.forPort(PORT_A).addService(serviceA).build().start();
        serverB =
                ServerBuilder.forPort(PORT_B)
                        .addService(ServerInterceptors.intercept(serviceB, counter))
                        .build()
                        .start();
        channelA = ManagedChannelBuilder.forTarget("localhost:" + PORT_A).usePlaintext().build();
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
            channelA.shutdownNow();
            channelA.awaitTermination(5, TimeUnit.SECONDS);
        }
        for (Server server : new Server[] {serverA, serverB}) {
            if (server == null || server.isTerminated()) {
                continue;
            }
            server.shutdownNow();
            server.awaitTermination(5, TimeUnit.SECONDS);
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

    /** Sends items owned by node B through node A and waits for every ack. */
    private static void forwardThroughA(String key, int items) throws InterruptedException {
        final List<AckMessage> acks = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch done = new CountDownLatch(1);
        StreamObserver<URLItem> input =
                URLFrontierGrpc.newStub(channelA)
                        .putURLs(
                                new StreamObserver<AckMessage>() {
                                    @Override
                                    public void onNext(AckMessage value) {
                                        acks.add(value);
                                    }

                                    @Override
                                    public void onError(Throwable t) {
                                        done.countDown();
                                    }

                                    @Override
                                    public void onCompleted() {
                                        done.countDown();
                                    }
                                });
        for (int i = 0; i < items; i++) {
            URLInfo info =
                    URLInfo.newBuilder()
                            .setUrl("https://" + key + "/" + i)
                            .setKey(key)
                            .setCrawlID("DEFAULT")
                            .build();
            input.onNext(
                    URLItem.newBuilder()
                            .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                            .build());
        }
        input.onCompleted();
        assertTrue(done.await(20, TimeUnit.SECONDS), "putURLs did not complete in time");
        assertEquals(items, acks.size(), "every item must be acked");
        for (AckMessage ack : acks) {
            assertNotEquals(AckMessage.Status.FAIL, ack.getStatus(), "forwarded item failed");
        }
    }

    private static void awaitClosed(int expected) throws InterruptedException {
        for (int i = 0; i < 100 && putURLsClosed.get() < expected; i++) {
            Thread.sleep(50);
        }
        assertEquals(expected, putURLsClosed.get(), "closed putURLs calls on the owner");
    }

    @Test
    void oneForwardingStreamIsSharedAndDiscardingItClosesTheCall() throws Exception {
        final String key = keyOwnedBy(1, "lifecycle");

        // several client streams, one after the other: they all forward through the
        // same stream, which stays open in between
        forwardThroughA(key, 5);
        forwardThroughA(key, 5);
        forwardThroughA(key, 5);

        assertEquals(1, putURLsStarted.get(), "the forwarding stream must be shared and reused");
        assertEquals(0, putURLsClosed.get(), "the forwarding stream must be kept open");

        // dropping the entry must terminate the call rather than leave it half-open
        serviceA.discardForwardingStream(1);
        awaitClosed(1);
        assertEquals(1, halfClosed.get(), "the forwarding call must be half-closed, not cancelled");
        assertEquals(List.of(Status.Code.OK), closeCodes, "the forwarding call must end cleanly");

        // and the next item opens a fresh one
        forwardThroughA(key, 5);
        assertEquals(2, putURLsStarted.get(), "a new stream must be created after a discard");
        assertEquals(1, putURLsClosed.get(), "the replacement must stay open");
    }
}
