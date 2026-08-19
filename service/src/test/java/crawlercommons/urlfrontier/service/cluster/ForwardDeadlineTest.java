// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.URLFrontierGrpc.URLFrontierBlockingStub;
import crawlercommons.urlfrontier.Urlfrontier.DeleteCrawlMessage;
import crawlercommons.urlfrontier.Urlfrontier.Local;
import crawlercommons.urlfrontier.Urlfrontier.LogLevelParams;
import crawlercommons.urlfrontier.Urlfrontier.Pagination;
import crawlercommons.urlfrontier.Urlfrontier.QueueWithinCrawlParams;
import crawlercommons.urlfrontier.service.rocksdb.ShardedRocksDBService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * Issue #174: the blocking calls forwarded to every node must be bounded by a deadline. A node that
 * is merely unreachable fails fast on connection refused; the case that used to hang the caller's
 * handler thread for ever is a node that accepts the connection and then never answers, which the
 * black hole below reproduces.
 */
class ForwardDeadlineTest {

    private static final int PORT_A = 7311;
    private static final int DEADLINE_SECONDS = 2;

    /** Generous enough that a hanging fan-out cannot be mistaken for a slow one. */
    private static final Duration MAX_WAIT = Duration.ofSeconds(DEADLINE_SECONDS + 15L);

    private static final String PATH_A = "./target/rocksdb-deadline-a";

    static ShardedRocksDBService serviceA;
    static Server serverA;
    static ManagedChannel channelA;
    static URLFrontierBlockingStub stubA;
    static BlackHole blackHole;

    /** Accepts connections and never says anything: every RPC sent to it stays pending. */
    private static final class BlackHole implements AutoCloseable {

        private final ServerSocket socket;
        private final List<Socket> accepted = new ArrayList<>();
        private final Thread thread;

        BlackHole() throws IOException {
            socket = new ServerSocket(0);
            thread =
                    new Thread(
                            () -> {
                                while (!socket.isClosed()) {
                                    try {
                                        Socket s = socket.accept();
                                        // keep a reference so the connection stays open
                                        synchronized (accepted) {
                                            accepted.add(s);
                                        }
                                    } catch (IOException e) {
                                        return;
                                    }
                                }
                            },
                            "blackhole-accept");
            thread.setDaemon(true);
            thread.start();
        }

        String target() {
            return "localhost:" + socket.getLocalPort();
        }

        @Override
        public void close() {
            try {
                socket.close();
            } catch (IOException e) {
                // closing the listening socket while the accept thread is parked in a
                // native accept() can fail to signal that thread (seen under emulation);
                // the socket is closing either way and the thread is a daemon
            }
            synchronized (accepted) {
                for (Socket s : accepted) {
                    try {
                        s.close();
                    } catch (IOException e) {
                        // closing on teardown
                    }
                }
            }
        }
    }

    @BeforeAll
    static void setup() throws IOException {
        FileUtils.deleteQuietly(new File(PATH_A));
        blackHole = new BlackHole();
        Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.path", PATH_A);
        conf.put("nodes", "localhost:" + PORT_A + "," + blackHole.target());
        conf.put("forward.deadline.seconds", Integer.toString(DEADLINE_SECONDS));
        serviceA = new ShardedRocksDBService(conf, "localhost", PORT_A);
        serverA = ServerBuilder.forPort(PORT_A).addService(serviceA).build().start();
        channelA = ManagedChannelBuilder.forTarget("localhost:" + PORT_A).usePlaintext().build();
        // a deadline well beyond the server-side one: the failure under test must come
        // from the forwarded call, not from the client giving up
        stubA = URLFrontierGrpc.newBlockingStub(channelA).withDeadlineAfter(60, TimeUnit.SECONDS);
    }

    @AfterAll
    static void teardown() throws Exception {
        if (channelA != null) {
            channelA.shutdownNow();
            channelA.awaitTermination(5, TimeUnit.SECONDS);
        }
        if (serverA != null) {
            serverA.shutdownNow();
            serverA.awaitTermination(5, TimeUnit.SECONDS);
        }
        try {
            if (serviceA != null) {
                serviceA.close();
            }
        } finally {
            try {
                if (blackHole != null) {
                    blackHole.close();
                }
            } finally {
                FileUtils.deleteQuietly(new File(PATH_A));
            }
        }
    }

    /** Fails if the call hangs, and returns the status it failed with. */
    private static Status.Code failsWithin(Executable call) {
        StatusRuntimeException e =
                assertTimeoutPreemptively(
                        MAX_WAIT,
                        () -> assertThrows(StatusRuntimeException.class, call),
                        "the forwarded call was not bounded by a deadline");
        return e.getStatus().getCode();
    }

    @Test
    void getStatsIsBounded() {
        assertEquals(
                Status.Code.DEADLINE_EXCEEDED,
                failsWithin(() -> stubA.getStats(QueueWithinCrawlParams.newBuilder().build())));
    }

    @Test
    void listQueuesIsBounded() {
        assertEquals(
                Status.Code.DEADLINE_EXCEEDED,
                failsWithin(() -> stubA.listQueues(Pagination.newBuilder().build())));
    }

    @Test
    void listCrawlsIsBounded() {
        assertEquals(
                Status.Code.DEADLINE_EXCEEDED,
                failsWithin(() -> stubA.listCrawls(Local.newBuilder().build())));
    }

    @Test
    void setLogLevelIsBounded() {
        assertEquals(
                Status.Code.DEADLINE_EXCEEDED,
                failsWithin(
                        () ->
                                stubA.setLogLevel(
                                        LogLevelParams.newBuilder()
                                                .setPackage("crawlercommons.urlfrontier")
                                                .setLevel(LogLevelParams.Level.INFO)
                                                .build())));
    }

    @Test
    void deleteQueueIsBounded() {
        assertEquals(
                Status.Code.DEADLINE_EXCEEDED,
                failsWithin(
                        () ->
                                stubA.deleteQueue(
                                        QueueWithinCrawlParams.newBuilder()
                                                .setKey("example.com")
                                                .build())));
    }

    @Test
    void deleteCrawlIsBounded() {
        assertEquals(
                Status.Code.DEADLINE_EXCEEDED,
                failsWithin(
                        () ->
                                stubA.deleteCrawl(
                                        DeleteCrawlMessage.newBuilder()
                                                .setValue("DEFAULT")
                                                .build())));
    }

    @Test
    void getActiveIsBounded() {
        // already had a deadline: guards against it being lost again
        assertEquals(
                Status.Code.DEADLINE_EXCEEDED,
                failsWithin(() -> stubA.getActive(Local.newBuilder().build())));
    }
}
