package org.chatapp.server.socket;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

// Class used for enabling SSL/TLS for socket server
public final class ServerTls {

    private ServerTls() {
    }

    public static SSLContext createContext(
            Path keystorePath,
            char[] password
    ) throws Exception {

        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream input = Files.newInputStream(keystorePath)) {
            keyStore.load(input, password);
        }

        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm()
                );

        keyManagerFactory.init(keyStore, password);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                keyManagerFactory.getKeyManagers(),
                null,
                null
        );

        return sslContext;
    }
}
