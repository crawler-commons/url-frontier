// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.Urlfrontier.Empty;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.Server;
import io.grpc.TlsChannelCredentials;
import io.grpc.TlsServerCredentials;
import io.grpc.stub.StreamObserver;
import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class ClientTlsTest {

    private static final File SERVER_CERT = resource("tls/server.pem");
    private static final File SERVER_KEY = resource("tls/server-key.pem");
    private static final File CLIENT_CERT = resource("tls/client.pem");
    private static final File CLIENT_KEY = resource("tls/client-key.pem");

    private Server server;

    @AfterEach
    void stopServer() throws InterruptedException {
        if (server != null) {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static Client parse(String... args) {
        Client client = new Client();
        new CommandLine(client).parseArgs(args);
        return client;
    }

    @Test
    void plaintextByDefault() {
        assertInstanceOf(InsecureChannelCredentials.class, parse().createCredentials());
    }

    @Test
    void tlsOptions() {
        TlsChannelCredentials tls =
                assertInstanceOf(TlsChannelCredentials.class, parse("--tls").createCredentials());
        assertNull(tls.getRootCertificates());
        assertNull(tls.getCertificateChain());

        // a trust collection implies TLS
        tls =
                assertInstanceOf(
                        TlsChannelCredentials.class,
                        parse("--tls-trust-cert", SERVER_CERT.getPath()).createCredentials());
        assertNotNull(tls.getRootCertificates());

        tls =
                assertInstanceOf(
                        TlsChannelCredentials.class,
                        parse(
                                        "--tls-cert",
                                        CLIENT_CERT.getPath(),
                                        "--tls-key",
                                        CLIENT_KEY.getPath())
                                .createCredentials());
        assertNotNull(tls.getCertificateChain());
        assertNotNull(tls.getPrivateKey());
    }

    @Test
    void certificateWithoutKeyIsRejected() {
        Client client = parse("--tls-cert", CLIENT_CERT.getPath());
        assertThrows(IllegalArgumentException.class, client::createCredentials);
    }

    @Test
    void mutualTlsRoundTrip() throws Exception {
        server =
                Grpc.newServerBuilderForPort(
                                0,
                                TlsServerCredentials.newBuilder()
                                        .keyManager(SERVER_CERT, SERVER_KEY)
                                        .trustManager(CLIENT_CERT)
                                        .clientAuth(TlsServerCredentials.ClientAuth.REQUIRE)
                                        .build())
                        .addService(
                                new URLFrontierGrpc.URLFrontierImplBase() {
                                    @Override
                                    public void listNodes(
                                            Empty request, StreamObserver<StringList> response) {
                                        response.onNext(StringList.getDefaultInstance());
                                        response.onCompleted();
                                    }
                                })
                        .build()
                        .start();

        assertEquals(
                0,
                run(
                        "--tls-trust-cert",
                        SERVER_CERT.getPath(),
                        "--tls-cert",
                        CLIENT_CERT.getPath(),
                        "--tls-key",
                        CLIENT_KEY.getPath()));

        // without a client certificate, and in plaintext, the server turns the client away
        assertNotEquals(0, run("--tls-trust-cert", SERVER_CERT.getPath()));
        assertNotEquals(0, run());
    }

    /** Runs ListNodes in a separate thread so that a regression fails instead of hanging. */
    private int run(String... tlsArgs) throws Exception {
        String[] args = new String[tlsArgs.length + 5];
        args[0] = "-t";
        args[1] = "localhost";
        args[2] = "-p";
        args[3] = Integer.toString(server.getPort());
        System.arraycopy(tlsArgs, 0, args, 4, tlsArgs.length);
        args[args.length - 1] = "ListNodes";
        Callable<Integer> command = () -> new CommandLine(new Client()).execute(args);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> result = executor.submit(command);
            return result.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private static File resource(String name) {
        try {
            return new File(ClientTlsTest.class.getClassLoader().getResource(name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
