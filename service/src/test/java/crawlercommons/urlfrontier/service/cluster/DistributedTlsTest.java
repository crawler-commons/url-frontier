// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;

import crawlercommons.urlfrontier.URLFrontierGrpc;
import crawlercommons.urlfrontier.Urlfrontier.QueueDelayParams;
import crawlercommons.urlfrontier.service.TlsConfig;
import crawlercommons.urlfrontier.service.rocksdb.ShardedRocksDBService;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Two sharded nodes which only accept TLS with a client certificate forward calls to each other.
 */
class DistributedTlsTest {

    private static final int PORT_A = 7311;
    private static final int PORT_B = 7312;
    private static final List<String> NODES = List.of("localhost:" + PORT_A, "localhost:" + PORT_B);
    private static final String PATH_A = "./target/rocksdb-tls-a";
    private static final String PATH_B = "./target/rocksdb-tls-b";

    private static final File SERVER_CERT = resource("tls/server.pem");
    private static final File SERVER_KEY = resource("tls/server-key.pem");
    static ShardedRocksDBService serviceA;
    static ShardedRocksDBService serviceB;
    static Server serverA;
    static Server serverB;
    static ManagedChannel channelA;

    @BeforeAll
    static void setup() throws Exception {
        FileUtils.deleteQuietly(new File(PATH_A));
        FileUtils.deleteQuietly(new File(PATH_B));
        Map<String, String> confA = conf(PATH_A);
        Map<String, String> confB = conf(PATH_B);
        serviceA = new ShardedRocksDBService(confA, "localhost", PORT_A);
        serviceB = new ShardedRocksDBService(confB, "localhost", PORT_B);
        serverA =
                Grpc.newServerBuilderForPort(PORT_A, TlsConfig.serverCredentials(confA))
                        .addService(serviceA)
                        .build()
                        .start();
        serverB =
                Grpc.newServerBuilderForPort(PORT_B, TlsConfig.serverCredentials(confB))
                        .addService(serviceB)
                        .build()
                        .start();
        // the test talks to node A like another node would
        channelA =
                Grpc.newChannelBuilder(NODES.get(0), TlsConfig.channelCredentials(confA)).build();
    }

    private static Map<String, String> conf(String path) {
        Map<String, String> conf = new HashMap<>();
        conf.put("rocksdb.path", path);
        conf.put("nodes", String.join(",", NODES));
        conf.put(TlsConfig.CERT_CHAIN, SERVER_CERT.getPath());
        conf.put(TlsConfig.PRIVATE_KEY, SERVER_KEY.getPath());
        conf.put(TlsConfig.TRUST_CERT_COLLECTION, SERVER_CERT.getPath());
        conf.put(TlsConfig.CLIENT_AUTH, "require");
        return conf;
    }

    @AfterAll
    static void teardown() throws Exception {
        if (channelA != null) {
            channelA.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        for (Server server : new Server[] {serverA, serverB}) {
            if (server != null) {
                server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
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

    @Test
    void callIsForwardedOverTls() {
        URLFrontierGrpc.newBlockingStub(channelA)
                .withDeadlineAfter(30, TimeUnit.SECONDS)
                .setDelay(
                        QueueDelayParams.newBuilder()
                                .setDelayRequestable(42)
                                .setLocal(false)
                                .build());

        assertEquals(42, serviceA.getDefaultDelayForQueues());
        // only reachable through the TLS channel from node A to node B
        assertEquals(42, serviceB.getDefaultDelayForQueues());
    }

    private static File resource(String name) {
        try {
            return new File(DistributedTlsTest.class.getClassLoader().getResource(name).toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
