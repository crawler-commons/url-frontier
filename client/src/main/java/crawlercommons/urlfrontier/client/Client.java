// SPDX-FileCopyrightText: 2020 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.client;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import java.io.File;
import java.io.IOException;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "Client",
        mixinStandardHelpOptions = true,
        versionProvider = VersionProvider.class,
        subcommands = {
            ListNodes.class,
            ListQueues.class,
            ListCrawls.class,
            ListURLs.class,
            GetStats.class,
            PutURLs.class,
            GetURLs.class,
            SetActive.class,
            GetActive.class,
            DeleteQueue.class,
            DeleteCrawl.class,
            SetLogLevel.class,
            SetCrawlLimit.class,
            GetURLStatus.class,
            CountURLs.class,
            PurgeURLs.class,
            DumpURLs.class
        },
        description = "Interacts with a URL Frontier from the command line")
public class Client {

    @Option(
            names = {"-t", "--host"},
            paramLabel = "STRING",
            defaultValue = "localhost",
            description = "URL Frontier hostname (defaults to 'localhost')")
    String hostname;

    @Option(
            names = {"-p", "--port"},
            defaultValue = "7071",
            paramLabel = "NUM",
            description = "URL Frontier port (default to 7071)")
    int port;

    @Option(
            names = {"--tls"},
            description =
                    "Connect with TLS; implied by the other --tls-* options (defaults to plaintext)")
    boolean tls;

    @Option(
            names = {"--tls-trust-cert"},
            paramLabel = "FILE",
            description =
                    "PEM file with the certificates trusted to sign the server certificate"
                            + " (defaults to the JVM trust store)")
    File trustCerts;

    @Option(
            names = {"--tls-cert"},
            paramLabel = "FILE",
            description = "PEM file with the client certificate chain, for mutual TLS")
    File certChain;

    @Option(
            names = {"--tls-key"},
            paramLabel = "FILE",
            description = "PKCS#8 PEM file with the private key of the client certificate")
    File privateKey;

    @Option(
            names = {"--tls-key-password"},
            paramLabel = "STRING",
            arity = "0..1",
            interactive = true,
            description =
                    "Password of the client private key; prompted for if the option is given"
                            + " without a value")
    String privateKeyPassword;

    public static void main(String... args) {
        CommandLine cli = new CommandLine(new Client());
        int exitCode = cli.execute(args);
        System.exit(exitCode);
    }

    /** Opens a channel to the Frontier, using TLS if one of the --tls options is set. */
    ManagedChannel createChannel() {
        return Grpc.newChannelBuilderForAddress(hostname, port, createCredentials()).build();
    }

    ChannelCredentials createCredentials() {
        if (!tls && trustCerts == null && certChain == null && privateKey == null) {
            return InsecureChannelCredentials.create();
        }
        if ((certChain == null) != (privateKey == null)) {
            throw new IllegalArgumentException(
                    "--tls-cert and --tls-key must be set together for mutual TLS");
        }
        TlsChannelCredentials.Builder builder = TlsChannelCredentials.newBuilder();
        try {
            if (trustCerts != null) {
                builder.trustManager(trustCerts);
            }
            if (certChain != null) {
                builder.keyManager(
                        certChain,
                        privateKey,
                        privateKeyPassword == null || privateKeyPassword.isEmpty()
                                ? null
                                : privateKeyPassword);
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException(
                    "Cannot read the TLS certificates or key: " + e.getMessage(), e);
        }
        return builder.build();
    }
}
