package org.chatapp.client.service.security;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

// Class used to set up client side tls
public final class ClientTls {
    private ClientTls() {
    }

    public static SSLContext create(boolean production) throws Exception {
        Certificate certificate;

        try (InputStream input = ClientTls.class
                .getResourceAsStream(production ? "/tls/prod/prod-cert.pem" : "/tls/dev/dev-cert.pem")) {

            if (input == null) {
                throw new IllegalStateException(
                        "Server trust certificate was not found"
                );
            }

            certificate = CertificateFactory
                    .getInstance("X.509")
                    .generateCertificate(input);
        }

        // Build a truststore in memory; no truststore password is needed.
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("chat-server", certificate);

        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm()
                );

        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                null,
                trustManagerFactory.getTrustManagers(),
                new SecureRandom()
        );

        return sslContext;
    }

}