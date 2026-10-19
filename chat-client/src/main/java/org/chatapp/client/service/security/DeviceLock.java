package org.chatapp.client.service.security;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

// Generating password for the refresh token keystore
public class DeviceLock {

    private static final int PASSWORD_BYTES = 32;

    private final Path passwordFile;

    public DeviceLock(Path passwordFile) {
        this.passwordFile = passwordFile;
    }

    public char[] getDeviceSpecificPassword() throws IOException {
        if (Files.exists(passwordFile)) {
            return Files.readString(passwordFile, StandardCharsets.UTF_8)
                    .toCharArray();
        }

        Path parent = passwordFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        byte[] randomBytes = new byte[PASSWORD_BYTES];
        new SecureRandom().nextBytes(randomBytes);

        String password = Base64.getEncoder()
                .encodeToString(randomBytes);

        try {
            Files.writeString(
                    passwordFile,
                    password,
                    StandardCharsets.UTF_8
            );

            // Restrict access on POSIX systems such as Linux.
            try {
                Files.setPosixFilePermissions(
                        passwordFile,
                        Set.of(
                                PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_WRITE
                        )
                );
            } catch (UnsupportedOperationException ignored) {
                // Non-POSIX filesystem; use platform-specific permissions
                // if you need equivalent protection on Windows.
            }

            return password.toCharArray();
        } catch (IOException e) {
            Files.deleteIfExists(passwordFile);
            throw e;
        }
    }
}