package org.chatapp.client.service.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public class CryptoUtils {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTE = 12;
    private static final int TAG_LENGTH_BIT = 128;


    // Encrypts the user token in AES(Advanced Encryption standard) GCM(Galois/Counter Mode)
    public byte[] encrypt(String data, SecretKey key) throws Exception {
        // Generate a random IV (Initialization vector, Initialization vector is a non-secret, random, or unpredictable
        // value, 12 byte here, used alongside a secret key to ensure that encrypting the same plaintext multiple times
        // with the same key produces unique ciphertext)
        byte[] iv = new byte[IV_LENGTH_BYTE];
        // Fills the byte array with random bytes
        new SecureRandom().nextBytes(iv);

        // Initialize cipher(A Cipher is an algorithm—a specific set of mathematical steps or rules—used to perform
        // encryption and decryption.) Here we pass the specified algorithm in our case AES with GCM
        Cipher cipher = Cipher.getInstance(ALGO);
        // Configuring parameters for the GCM
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        // Initializes the encryption mode, providing secret key and GCM parameters, ready for encryption
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        // Encrypts the data
        byte[] cipherText = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // Prefix the IV to the CipherText so we can use it for decryption later
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
        byteBuffer.put(iv);
        byteBuffer.put(cipherText);
        return byteBuffer.array();
    }


    // Decrypts the user token
    public String decrypt(byte[] encryptedData, SecretKey key) throws Exception {
        // Extract IV (first 12 bytes)
        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
        byte[] iv = new byte[IV_LENGTH_BYTE];
        byteBuffer.get(iv);

        // Extract CipherText (remaining bytes)
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        // Initialize cipher and get gcp parameters
        Cipher cipher = Cipher.getInstance(ALGO);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        // Decrypt the encrypted text
        byte[] plainText = cipher.doFinal(cipherText);

        // Return the text as UTF_8 charset String
        return new String(plainText, StandardCharsets.UTF_8);
    }



}
