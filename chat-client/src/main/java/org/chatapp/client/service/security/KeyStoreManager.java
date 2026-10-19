package org.chatapp.client.service.security;

import org.chatapp.client.service.AppPaths;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;

public class KeyStoreManager {

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEY_ALIAS = "AutoLoginKey";
    private static final String PASSWORD_FILE = "device-password";
    private final String keyStorePath;

    public KeyStoreManager(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public SecretKey getPayloadKey() throws Exception {
        // Dynamically generate password
        char[] devicePassword = new DeviceLock(AppPaths.getAppDataDirectory().resolve(PASSWORD_FILE)).getDeviceSpecificPassword();

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        File file = new File(keyStorePath);

        // Load or Init using the device password
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                keyStore.load(fis, devicePassword);
            }
        } else {
            keyStore.load(null, devicePassword);
        }

        // Retrieve or Create Key
        if (keyStore.containsAlias(KEY_ALIAS)) {
            KeyStore.PasswordProtection keyPass =
                    new KeyStore.PasswordProtection(devicePassword);
            KeyStore.SecretKeyEntry entry =
                    (KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, keyPass);
            return entry.getSecretKey();
        }

        // Generate new if missing
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();

        KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.PasswordProtection keyPass =
                new KeyStore.PasswordProtection(devicePassword);

        // Saves entry into keystore
        keyStore.setEntry(KEY_ALIAS, entry, keyPass);

        // Write keystore to the file and protect it with given password
        try (FileOutputStream fos = new FileOutputStream(keyStorePath)) {
            keyStore.store(fos, devicePassword);
        }

        return secretKey;
    }

}
