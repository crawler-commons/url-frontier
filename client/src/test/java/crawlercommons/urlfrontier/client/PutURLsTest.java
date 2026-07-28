// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.Urlfrontier.AckMessage;
import crawlercommons.urlfrontier.Urlfrontier.URLItem;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/** Checks that PutURLs terminates and reports a failure when the stream breaks, see #169. */
class PutURLsTest {

    private static final int URL_COUNT = 50;

    private Server server;

    @TempDir Path tempDir;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    void returnsZeroWhenAllTheURLsAreAcked() throws Exception {
        // never fails
        startServer(Integer.MAX_VALUE);
        assertEquals(0, runClient());
    }

    @Test
    void doesNotHangWhenTheServerFails() throws Exception {
        // the acks stop coming after the 5th URL
        startServer(5);
        assertEquals(1, runClient());
    }

    @Test
    void returnsNonZeroWhenTheFileCannotBeRead() throws Exception {
        startServer(Integer.MAX_VALUE);
        assertEquals(1, run(tempDir.resolve("does-not-exist.txt")));
    }

    private int runClient() throws Exception {
        Path input = tempDir.resolve("urls.txt");
        List<String> urls = new ArrayList<>();
        for (int i = 0; i < URL_COUNT; i++) {
            urls.add("http://example.com/" + i);
        }
        Files.write(input, urls);
        return run(input);
    }

    /** Runs the command in a separate thread so that a regression fails instead of hanging. */
    private int run(Path input) throws Exception {
        Callable<Integer> command =
                () ->
                        new CommandLine(new Client())
                                .execute(
                                        "-t",
                                        "localhost",
                                        "-p",
                                        Integer.toString(server.getPort()),
                                        "PutURLs",
                                        "-f",
                                        input.toString());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> result = executor.submit(command);
            return result.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    /** Starts a frontier which breaks the stream after having acked maxAcks items. */
    private void startServer(final int maxAcks) throws IOException {
        server =
                ServerBuilder.forPort(0)
                        .addService(
                                new URLFrontierGrpc.URLFrontierImplBase() {
                                    @Override
                                    public StreamObserver<URLItem> putURLs(
                                            StreamObserver<AckMessage> responseObserver) {
                                        return new StreamObserver<>() {

                                            int received = 0;
                                            boolean broken = false;

                                            @Override
                                            public void onNext(URLItem value) {
                                                if (broken) {
                                                    return;
                                                }
                                                if (++received > maxAcks) {
                                                    broken = true;
                                                    responseObserver.onError(
                                                            Status.INTERNAL
                                                                    .withDescription("boom")
                                                                    .asRuntimeException());
                                                    return;
                                                }
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
                                                if (!broken) {
                                                    responseObserver.onCompleted();
                                                }
                                            }
                                        };
                                    }
                                })
                        .build()
                        .start();
    }
}
