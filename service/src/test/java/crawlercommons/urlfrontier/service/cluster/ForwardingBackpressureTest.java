// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.QueueWithinCrawl;
import crawlercommons.urlfrontier.service.rocksdb.ShardedRocksDBService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ServerCallStreamObserver;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Pacing of the putURLs stream node A opens onto node B (issue #208). An owner which stops draining
 * must push back on the clients of A rather than have its items accumulate in the outbound buffers
 * of A, so the amount buffered is bounded by the transport and not by what the clients send.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ForwardingBackpressureTest {

    private static final int PORT_A = 7313;
    private static final int PORT_B = 7314;
    private static final List<String> NODES = List.of("localhost:" + PORT_A, "localhost:" + PORT_B);
    private static final String PATH_A = "./target/rocksdb-backpressure-a";

    /** long enough to ride out a hiccup, short enough for a test to wait for it */
    private static final int READY_TIMEOUT_MS = 2000;

    /** big items so that the flow control window of the connection fills within the burst */
    private static final int URL_PADDING = 16 * 1024;

    static DistributedFrontierService serviceA;
    static Server serverA;
    static Server serverB;
    static ManagedChannel channelA;
    static StallingOwner owner;

    /** Node B: acks everything, or stops reading its stream altogether. */
    private static final class StallingOwner extends URLFrontierGrpc.URLFrontierImplBase {

        volatile boolean stalling = false;

        final AtomicInteger received = new AtomicInteger();

        @Override
        public StreamObserver<URLItem> putURLs(StreamObserver<AckMessage> responseObserver) {
            if (stalling) {
                // never request a message and never ack one: the items node A writes stay
                // on the wire until the flow control window is full, which is what makes
                // its stream unwritable
                ((ServerCallStreamObserver<AckMessage>) responseObserver).disableAutoRequest();
                return new StreamObserver<>() {
                    @Override
                    public void onNext(URLItem value) {}

                    @Override
                    public void onError(Throwable t) {}

                    @Override
                    public void onCompleted() {}
                };
            }
            return new StreamObserver<>() {
                @Override
                public void onNext(URLItem value) {
                    received.incrementAndGet();
                    responseObserver.onNext(
                            AckMessage.newBuilder()
                                    .setID(value.getID())
                                    .setStatus(AckMessage.Status.OK)
                                    .build());
                }

                @Override
                public void onError(Throwable t) {}

                @Override
                public void onCompleted() {
                    responseObserver.onCompleted();
                }
            };
        }
    }

    @BeforeAll
    static void setup() throws IOException {
        FileUtils.deleteQuietly(new File(PATH_A));
        Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.path", PATH_A);
        conf.put("nodes", String.join(",", NODES));
        conf.put("forward.ready.timeout.ms", Integer.toString(READY_TIMEOUT_MS));
        serviceA = new ShardedRocksDBService(conf, "localhost", PORT_A);
        owner = new StallingOwner();
        serverA = ServerBuilder.forPort(PORT_A).addService(serviceA).build().start();
        serverB = ServerBuilder.forPort(PORT_B).addService(owner).build().start();
        channelA = ManagedChannelBuilder.forTarget("localhost:" + PORT_A).usePlaintext().build();
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
            FileUtils.deleteQuietly(new File(PATH_A));
        }
    }

    /** Finds a key owned by node B, distinct per label. */
    private static String keyOwnedByB(String label) {
        for (int i = 0; i < 10_000; i++) {
            String key = label + "-" + i + ".test";
            if (DistributedFrontierService.partitionFor(QueueWithinCrawl.get(key, "DEFAULT"), NODES)
                    == 1) {
                return key;
            }
        }
        throw new IllegalStateException("no key found for node B");
    }

    /** Opens a client stream onto node A, collecting the acks it sends back. */
    private static StreamObserver<URLItem> openStream(List<AckMessage> acks, CountDownLatch done) {
        return URLFrontierGrpc.newStub(channelA)
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
    }

    /** An item owned by node B, padded so that a burst of them fills the connection. */
    private static URLItem itemFor(String key, int i) {
        final String padding = "x".repeat(URL_PADDING);
        URLInfo info =
                URLInfo.newBuilder()
                        .setUrl("https://" + key + "/" + i + "?p=" + padding)
                        .setKey(key)
                        .setCrawlID("DEFAULT")
                        .build();
        return URLItem.newBuilder()
                .setID(Integer.toString(i))
                .setDiscovered(DiscoveredURLItem.newBuilder().setInfo(info).build())
                .build();
    }

    /** Sends items owned by node B through node A and returns the acks, in order. */
    private static List<AckMessage> forwardThroughA(String key, int items, int timeoutSeconds)
            throws InterruptedException {
        final List<AckMessage> acks = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch done = new CountDownLatch(1);
        StreamObserver<URLItem> input = openStream(acks, done);
        for (int i = 0; i < items; i++) {
            input.onNext(itemFor(key, i));
        }
        input.onCompleted();
        assertTrue(
                done.await(timeoutSeconds, TimeUnit.SECONDS), "putURLs did not complete in time");
        return acks;
    }

    private static long failed(List<AckMessage> acks) {
        return acks.stream().filter(a -> a.getStatus() == AckMessage.Status.FAIL).count();
    }

    @Test
    @Order(1)
    void arespondingOwnerTakesEverything() throws Exception {
        final String key = keyOwnedByB("responsive");
        final List<AckMessage> acks = forwardThroughA(key, 200, 30);
        assertEquals(200, acks.size(), "every item must be acked");
        assertEquals(0, failed(acks), "nothing may fail while the owner keeps up");
        assertEquals(200, owner.received.get(), "every item must reach the owner");
    }

    @Test
    @Order(2)
    void aStalledOwnerIsPacedInsteadOfBuffered() throws Exception {
        // the stream of the previous test is still cached: drop it so that the next item
        // opens one onto the stalling handler
        serviceA.discardForwardingStream(1);
        owner.stalling = true;
        owner.received.set(0);

        final String key = keyOwnedByB("stalled");
        final int items = 400;

        final List<AckMessage> acks = Collections.synchronizedList(new ArrayList<>());
        final CountDownLatch done = new CountDownLatch(1);
        final StreamObserver<URLItem> input = openStream(acks, done);

        final long start = System.nanoTime();
        for (int i = 0; i < items; i++) {
            input.onNext(itemFor(key, i));
        }
        input.onCompleted();

        // the burst is answered while the owner is still stalled: whatever made it onto
        // the wire before the stream became unwritable is not acked back until the
        // in-process cache expires it a minute later, so the stream cannot complete here
        awaitQuiescence(acks);
        final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        final long failures = failed(acks);
        assertEquals(failures, acks.size(), "a stalled owner cannot ack anything back");
        assertTrue(failures > 0, "a stalled owner must fail items back rather than buffer them");

        // what got through is what the transport accepted before the stream became
        // unwritable: a bound coming from the connection, not from what the client sent
        final long accepted = items - failures;
        assertTrue(
                accepted < items / 2,
                "buffering must be bounded by the transport, "
                        + accepted
                        + " of "
                        + items
                        + " out");

        // the timeout is paid once for the stall, not once per item, so that a wedged
        // owner cannot hold a client stream hostage
        assertTrue(
                elapsedMillis < READY_TIMEOUT_MS * 5L,
                "the burst took " + elapsedMillis + " ms, one timeout per item");

        // the client is left waiting for the items which did go out, as it would be in
        // production until they expire; nothing here depends on that happening
        ((ClientCallStreamObserver<URLItem>) input).cancel("end of test", null);
        assertTrue(done.await(10, TimeUnit.SECONDS), "the cancelled stream must terminate");
    }

    /** Waits for the acks to stop coming, which they do once the burst has been answered. */
    private static void awaitQuiescence(List<AckMessage> acks) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
        int last = -1;
        int stable = 0;
        while (System.nanoTime() < deadline) {
            Thread.sleep(250);
            final int size = acks.size();
            stable = size == last ? stable + 1 : 0;
            last = size;
            // quiet for longer than a timeout: nothing is still waiting on the stream
            if (size > 0 && stable * 250 > READY_TIMEOUT_MS) {
                return;
            }
        }
        throw new AssertionError(
                "the burst was not answered: " + acks.size() + " acks and still coming");
    }
}
