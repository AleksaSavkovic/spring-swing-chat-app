package org.chatapp.client.service;

import java.nio.file.Path;

public final class AppPaths {
    private static final String APP_NAME = "ChatApp";

    private AppPaths() {}

    // Gets app data directory path for different operating systems
    public static Path getAppDataDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        // Windows path
        if (os.contains("win")) {
            return getWindowsPath();
        }
        // MacOS path
        if (os.contains("mac")) {
            return getMacPath();
        }
        // Linux path
        return getLinuxPath();
    }

    // Returns linux path
    private static Path getLinuxPath() {
        return Path.of(
                System.getProperty("user.home"),
                ".local",
                "share",
                APP_NAME
        );
    }

    // Returns mac path
    private static Path getMacPath() {
        return Path.of(
                System.getProperty("user.home"),
                "Library",
                "Application Support",
                APP_NAME
        );
    }

    // Returns windows path
    private static Path getWindowsPath() {
        String localAppData = System.getenv("LOCALAPPDATA");

        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, APP_NAME);
        }

        return Path.of(
                System.getProperty("user.home"),
                "AppData",
                "Local",
                APP_NAME
        );
    }

    // Returns a folder for storing current users' files
    public static Path getUserFilesDirectory(String userId) {
        return getAppDataDirectory()
                .resolve(userId + "-files");
    }
}
