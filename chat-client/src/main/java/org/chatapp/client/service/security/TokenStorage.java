package org.chatapp.client.service.security;

import javax.crypto.SecretKey;
import java.io.File;
import java.nio.file.Files;

public class TokenStorage {

    private final File tokenFile;
    private final KeyStoreManager keyManager;
    private final CryptoUtils crypto;

    public TokenStorage(String filePath, KeyStoreManager keyManager) {
        this.tokenFile = new File(filePath);
        this.keyManager = keyManager;
        this.crypto = new CryptoUtils();
    }

    // Save refresh token to the file, encrypting it using the key obtained from the keystore
    public void saveRefreshToken(String token) throws Exception {
        SecretKey key = keyManager.getPayloadKey();
        byte[] encrypted = crypto.encrypt(token, key);
        Files.write(tokenFile.toPath(), encrypted);
    }

    // Checks if the file containing refresh token exists, and if it does, it gets the encryption key from the keystore
    // reads the content of the file as a byte array and then decrypts it using our key
    public String loadRefreshToken() throws Exception {
        if (!tokenFile.exists()) return null;

        SecretKey key = keyManager.getPayloadKey();
        byte[] fileContent = Files.readAllBytes(tokenFile.toPath());
        return crypto.decrypt(fileContent, key);
    }
}
