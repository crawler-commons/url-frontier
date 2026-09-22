// SPDX-FileCopyrightText: 2026 Crawler-commons
// SPDX-License-Identifier: Apache-2.0

package crawlercommons.urlfrontier.service;

import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;
import io.grpc.InsecureServerCredentials;
import io.grpc.ServerCredentials;
import io.grpc.TlsChannelCredentials;
import io.grpc.TlsServerCredentials;
import io.grpc.TlsServerCredentials.ClientAuth;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the gRPC credentials of the server and of the channels between the nodes of a cluster from
 * the configuration. TLS is used when both {@link #CERT_CHAIN} and {@link #PRIVATE_KEY} are set,
 * otherwise the server and the channels are plaintext.
 */
public final class TlsConfig {

    /** PEM file with the certificate chain of the server. */
    public static final String CERT_CHAIN = "server.tls.cert.chain";

    /** PKCS#8 PEM file with the private key of the server certificate. */
    public static final String PRIVATE_KEY = "server.tls.private.key";

    /** Password of {@link #PRIVATE_KEY}, if it is encrypted. */
    public static final String PRIVATE_KEY_PASSWORD = "server.tls.private.key.password";

    /**
     * PEM file with the certificates trusted to sign the client certificates and the certificates
     * of the other nodes of a cluster. The JVM trust store is used if not set.
     */
    public static final String TRUST_CERT_COLLECTION = "server.tls.trust.cert.collection";

    /** Whether clients must present a certificate: none (default), optional or require. */
    public static final String CLIENT_AUTH = "server.tls.client.auth";

    private TlsConfig() {}

    /** Whether TLS is configured, i.e. whether the certificate chain and the key are both set. */
    public static boolean isEnabled(Map<String, String> configuration) {
        boolean hasCertChain = isSet(configuration, CERT_CHAIN);
        boolean hasPrivateKey = isSet(configuration, PRIVATE_KEY);
        if (hasCertChain != hasPrivateKey) {
            throw new IllegalArgumentException(
                    CERT_CHAIN
                            + " and "
                            + PRIVATE_KEY
                            + " must be set together to enable TLS, only "
                            + (hasCertChain ? CERT_CHAIN : PRIVATE_KEY)
                            + " is set");
        }
        return hasCertChain;
    }

    /**
     * Credentials of the server: plaintext unless TLS is configured.
     *
     * @throws IllegalArgumentException if the configuration is incomplete or invalid, or if one of
     *     the configured files cannot be read
     */
    public static ServerCredentials serverCredentials(Map<String, String> configuration) {
        if (!isEnabled(configuration)) {
            return InsecureServerCredentials.create();
        }

        TlsServerCredentials.Builder builder = TlsServerCredentials.newBuilder();
        String certChain = configuration.get(CERT_CHAIN);
        String privateKey = configuration.get(PRIVATE_KEY);
        try {
            builder.keyManager(
                    new File(certChain),
                    new File(privateKey),
                    emptyToNull(configuration.get(PRIVATE_KEY_PASSWORD)));
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException(
                    "Cannot read the certificate chain " + certChain + " or the key " + privateKey,
                    e);
        }

        if (isSet(configuration, TRUST_CERT_COLLECTION)) {
            String trustCerts = configuration.get(TRUST_CERT_COLLECTION);
            try {
                builder.trustManager(new File(trustCerts));
            } catch (IOException | RuntimeException e) {
                throw new IllegalArgumentException(
                        "Cannot read the certificates in "
                                + TRUST_CERT_COLLECTION
                                + ": "
                                + trustCerts,
                        e);
            }
        }

        builder.clientAuth(clientAuth(configuration));
        return builder.build();
    }

    /**
     * Credentials of the channels to the other nodes of a cluster: plaintext unless TLS is
     * configured. With TLS the certificates of the other nodes are checked against {@link
     * #TRUST_CERT_COLLECTION}, or against the JVM trust store if that key is not set, and the
     * certificate of this node is sent as a client certificate so that the other nodes accept the
     * call when they require one.
     *
     * @throws IllegalArgumentException if the configuration is incomplete, or if one of the
     *     configured files cannot be read
     */
    public static ChannelCredentials channelCredentials(Map<String, String> configuration) {
        if (!isEnabled(configuration)) {
            return InsecureChannelCredentials.create();
        }

        TlsChannelCredentials.Builder builder = TlsChannelCredentials.newBuilder();
        String certChain = configuration.get(CERT_CHAIN);
        String privateKey = configuration.get(PRIVATE_KEY);
        try {
            builder.keyManager(
                    new File(certChain),
                    new File(privateKey),
                    emptyToNull(configuration.get(PRIVATE_KEY_PASSWORD)));
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException(
                    "Cannot read the certificate chain " + certChain + " or the key " + privateKey,
                    e);
        }

        if (isSet(configuration, TRUST_CERT_COLLECTION)) {
            String trustCerts = configuration.get(TRUST_CERT_COLLECTION);
            try {
                builder.trustManager(new File(trustCerts));
            } catch (IOException | RuntimeException e) {
                throw new IllegalArgumentException(
                        "Cannot read the certificates in "
                                + TRUST_CERT_COLLECTION
                                + ": "
                                + trustCerts,
                        e);
            }
        }

        return builder.build();
    }

    static ClientAuth clientAuth(Map<String, String> configuration) {
        String value = configuration.getOrDefault(CLIENT_AUTH, "none").trim();
        switch (value.toLowerCase(Locale.ROOT)) {
            case "":
            case "none":
                return ClientAuth.NONE;
            case "optional":
                return ClientAuth.OPTIONAL;
            case "require":
                return ClientAuth.REQUIRE;
            default:
                throw new IllegalArgumentException(
                        "Invalid value for "
                                + CLIENT_AUTH
                                + ": "
                                + value
                                + ", expected none, optional or require");
        }
    }

    private static boolean isSet(Map<String, String> configuration, String key) {
        String value = configuration.get(key);
        return value != null && !value.isBlank();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
