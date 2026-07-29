// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierStub;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage.Status;
import crawlercommons.urlfrontier.Urlfrontier.DiscoveredURLItem;
import crawlercommons.urlfrontier.Urlfrontier.URLInfo;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import io.grpc.ForwardingServerCallListener;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The server must stop reading from a putURLs stream once putURLs.max.inflight URLs have been
 * received but not acked, and read one more for every ack sent. Runs real gRPC calls over the
 * in-process transport, which honours the same flow control requests as a socket.
 */
class PutURLsBackpressureTest {

    /** A frontier whose writes block until the test releases them */
    private static final class BlockedFrontier extends MemoryFrontierService {

        final CountDownLatch release = new CountDownLatch(1);

        BlockedFrontier(Map<String, String> conf) {
            super(conf, "localhost", 0);
        }

        @Override
        protected Status putURLItem(URLItem value) {
            try {
                release.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return super.putURLItem(value);
        }
    }

    private Server server;
    private ManagedChannel channel;
    private BlockedFrontier frontier;

    @AfterEach
    void teardown() throws Exception {
        frontier.release.countDown();
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
        frontier.close();
    }

    /** Counts the messages gRPC hands over to the service, i.e. the requests it granted */
    private static ServerInterceptor countingDeliveries(AtomicInteger delivered) {
        return new ServerInterceptor() {
            @Override
            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                    ServerCall<ReqT, RespT> call,
                    Metadata headers,
                    ServerCallHandler<ReqT, RespT> next) {
                return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(
                        next.startCall(call, headers)) {
                    @Override
                    public void onMessage(ReqT message) {
                        delivered.incrementAndGet();
                        super.onMessage(message);
                    }
                };
            }
        };
    }

    private URLFrontierStub startAndConnect(int maxInFlight, AtomicInteger delivered)
            throws Exception {
        final Map<String, String> conf = new HashMap<>();
        conf.put("putURLs.max.inflight", Integer.toString(maxInFlight));
        conf.put("write.thread.num", "2");
        frontier = new BlockedFrontier(conf);

        final String name = InProcessServerBuilder.generateName();
        server =
                InProcessServerBuilder.forName(name)
                        .addService(
                                ServerInterceptors.intercept(
                                        frontier, countingDeliveries(delivered)))
                        .build()
                        .start();
        channel = InProcessChannelBuilder.forName(name).build();
        return URLFrontierGrpc.newStub(channel);
    }

    private static URLItem item(int i) {
        return URLItem.newBuilder()
                .setDiscovered(
                        DiscoveredURLItem.newBuilder()
                                .setInfo(
                                        URLInfo.newBuilder()
                                                .setUrl("http://host" + i + ".com/page")
                                                .build())
                                .build())
                .build();
    }

    /** Waits until the value stops changing for a little while and returns it */
    private static int settle(AtomicInteger value) throws InterruptedException {
        int last = -1;
        for (int i = 0; i < 50; i++) {
            int current = value.get();
            if (current == last) {
                return current;
            }
            last = current;
            Thread.sleep(100);
        }
        return last;
    }

    @Test
    void deliveriesStopAtTheBudgetAndResumeWithTheAcks() throws Exception {

        final int maxInFlight = 8;
        final int total = 100;

        final AtomicInteger delivered = new AtomicInteger();
        final URLFrontierStub stub = startAndConnect(maxInFlight, delivered);

        final CountDownLatch completed = new CountDownLatch(1);
        final AtomicInteger acked = new AtomicInteger();

        final StreamObserver<URLItem> stream =
                stub.putURLs(
                        new StreamObserver<AckMessage>() {
                            @Override
                            public void onNext(AckMessage value) {
                                acked.incrementAndGet();
                            }

                            @Override
                            public void onError(Throwable t) {
                                completed.countDown();
                            }

                            @Override
                            public void onCompleted() {
                                completed.countDown();
                            }
                        });

        for (int i = 0; i < total; i++) {
            stream.onNext(item(i));
        }
        stream.onCompleted();

        // with every write blocked no ack can go out, so the server must stop
        // accepting once it holds maxInFlight URLs; the rest stays undelivered
        assertEquals(maxInFlight, settle(delivered));
        assertEquals(0, acked.get());

        // each ack now releases one more, until everything has been processed
        frontier.release.countDown();

        assertTrue(completed.await(30, TimeUnit.SECONDS), "stream did not complete");
        assertEquals(total, delivered.get());
        assertEquals(total, acked.get());
    }

    @Test
    void zeroDisablesTheLimit() throws Exception {

        final int total = 100;

        final AtomicInteger delivered = new AtomicInteger();
        final URLFrontierStub stub = startAndConnect(0, delivered);

        final CountDownLatch completed = new CountDownLatch(1);
        final AtomicInteger acked = new AtomicInteger();

        final StreamObserver<URLItem> stream =
                stub.putURLs(
                        new StreamObserver<AckMessage>() {
                            @Override
                            public void onNext(AckMessage value) {
                                acked.incrementAndGet();
                            }

                            @Override
                            public void onError(Throwable t) {
                                completed.countDown();
                            }

                            @Override
                            public void onCompleted() {
                                completed.countDown();
                            }
                        });

        for (int i = 0; i < total; i++) {
            stream.onNext(item(i));
        }
        stream.onCompleted();

        // without a limit the server keeps reading even though nothing is acked
        assertEquals(total, settle(delivered));
        assertEquals(0, acked.get());

        frontier.release.countDown();
        assertTrue(completed.await(30, TimeUnit.SECONDS), "stream did not complete");
        assertEquals(total, acked.get());
    }
}
