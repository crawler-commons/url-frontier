// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.Urlfrontier.Local;
import crawlercommons.urlfrontier.Urlfrontier.StringList;
import crawlercommons.urlfrontier.service.memory.MemoryFrontierService;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.InsecureServerCredentials;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.StatusRuntimeException;
import io.grpc.TlsChannelCredentials;
import io.grpc.TlsServerCredentials;
import io.grpc.TlsServerCredentials.ClientAuth;
import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TlsConfigTest {

    private static final File SERVER_CERT = resource("tls/server.pem");
    private static final File SERVER_KEY = resource("tls/server-key.pem");
    private static final File CLIENT_CERT = resource("tls/client.pem");
    private static final File CLIENT_KEY = resource("tls/client-key.pem");

    private Server server;
    private ManagedChannel channel;

    @AfterEach
    void shutdown() throws InterruptedException {
        if (channel != null) {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static Map<String, String> tlsConf() {
        Map<String, String> conf = new HashMap<>();
        conf.put(TlsConfig.CERT_CHAIN, SERVER_CERT.getPath());
        conf.put(TlsConfig.PRIVATE_KEY, SERVER_KEY.getPath());
        return conf;
    }

    @Test
    void plaintextByDefault() {
        Map<String, String> conf = new HashMap<>();
        assertInstanceOf(InsecureServerCredentials.class, TlsConfig.serverCredentials(conf));
        assertInstanceOf(InsecureChannelCredentials.class, TlsConfig.channelCredentials(conf));
    }

    @Test
    void tlsWhenCertificateAndKeyAreSet() {
        TlsServerCredentials server =
                assertInstanceOf(
                        TlsServerCredentials.class, TlsConfig.serverCredentials(tlsConf()));
        assertNotNull(server.getCertificateChain());
        assertNotNull(server.getPrivateKey());
        assertEquals(ClientAuth.NONE, server.getClientAuth());

        // the node presents its own certificate to the other nodes of the cluster
        TlsChannelCredentials channel =
                assertInstanceOf(
                        TlsChannelCredentials.class, TlsConfig.channelCredentials(tlsConf()));
        assertNotNull(channel.getCertificateChain());
        assertNotNull(channel.getPrivateKey());
    }

    @Test
    void certificateWithoutKeyIsRejected() {
        Map<String, String> conf = new HashMap<>();
        conf.put(TlsConfig.CERT_CHAIN, SERVER_CERT.getPath());
        assertThrows(IllegalArgumentException.class, () -> TlsConfig.serverCredentials(conf));
        assertThrows(IllegalArgumentException.class, () -> TlsConfig.channelCredentials(conf));
    }

    @Test
    void unreadableFilesAreRejected() {
        Map<String, String> conf = tlsConf();
        conf.put(TlsConfig.PRIVATE_KEY, "/does/not/exist.pem");
        assertThrows(IllegalArgumentException.class, () -> TlsConfig.serverCredentials(conf));

        Map<String, String> trust = tlsConf();
        trust.put(TlsConfig.TRUST_CERT_COLLECTION, "/does/not/exist.pem");
        assertThrows(IllegalArgumentException.class, () -> TlsConfig.serverCredentials(trust));
        assertThrows(IllegalArgumentException.class, () -> TlsConfig.channelCredentials(trust));
    }

    @Test
    void clientAuthValues() {
        Map<String, String> conf = new HashMap<>();
        assertEquals(ClientAuth.NONE, TlsConfig.clientAuth(conf));
        conf.put(TlsConfig.CLIENT_AUTH, "Optional");
        assertEquals(ClientAuth.OPTIONAL, TlsConfig.clientAuth(conf));
        conf.put(TlsConfig.CLIENT_AUTH, "require");
        assertEquals(ClientAuth.REQUIRE, TlsConfig.clientAuth(conf));
        conf.put(TlsConfig.CLIENT_AUTH, "always");
        assertThrows(IllegalArgumentException.class, () -> TlsConfig.clientAuth(conf));
    }

    @Test
    void tlsRoundTrip() throws Exception {
        startServer(tlsConf());

        listCrawls(trustingServer().build());

        // a plaintext client cannot talk to a TLS server
        assertThrows(
                StatusRuntimeException.class,
                () -> listCrawls(InsecureChannelCredentials.create()));
    }

    @Test
    void mutualTlsRoundTrip() throws Exception {
        Map<String, String> conf = tlsConf();
        conf.put(TlsConfig.TRUST_CERT_COLLECTION, CLIENT_CERT.getPath());
        conf.put(TlsConfig.CLIENT_AUTH, "require");
        startServer(conf);

        listCrawls(trustingServer().keyManager(CLIENT_CERT, CLIENT_KEY).build());

        // a client without a certificate is turned away
        assertThrows(StatusRuntimeException.class, () -> listCrawls(trustingServer().build()));
    }

    private static TlsChannelCredentials.Builder trustingServer() throws Exception {
        return TlsChannelCredentials.newBuilder().trustManager(SERVER_CERT);
    }

    private void startServer(Map<String, String> conf) throws Exception {
        server =
                Grpc.newServerBuilderForPort(0, TlsConfig.serverCredentials(conf))
                        .addService(new MemoryFrontierService(new HashMap<>(), "localhost", 0))
                        .build()
                        .start();
    }

    private void listCrawls(ChannelCredentials credentials) {
        if (channel != null) {
            channel.shutdownNow();
        }
        channel =
                Grpc.newChannelBuilderForAddress("localhost", server.getPort(), credentials)
                        .build();
        StringList crawls =
                URLFrontierGrpc.newBlockingStub(channel)
                        .withDeadlineAfter(10, TimeUnit.SECONDS)
                        .listCrawls(Local.getDefaultInstance());
        assertNotNull(crawls);
    }

    private static File resource(String name) {
        try {
            return new File(TlsConfigTest.class.getClassLoader().getResource(name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
