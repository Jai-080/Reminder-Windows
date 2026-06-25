package com.reminder.desktop.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class TokenStorage {
    private static final String FILE_NAME = ".reminder_desktop.properties";
    private static final File STORAGE_FILE = new File(System.getProperty("user.home"), FILE_NAME);
    private static final Properties properties = new Properties();

    static {
        load();
    }

    private static synchronized void load() {
        if (STORAGE_FILE.exists()) {
            try (FileInputStream fis = new FileInputStream(STORAGE_FILE)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading token storage: " + e.getMessage());
            }
        }
    }

    private static synchronized void save() {
        try (FileOutputStream fos = new FileOutputStream(STORAGE_FILE)) {
            properties.store(fos, "Reminder Desktop Local Token Storage");
        } catch (IOException e) {
            System.err.println("Error saving token storage: " + e.getMessage());
        }
    }

    public static synchronized void saveSession(String accessToken, String refreshToken, Long userId, String username, boolean rememberMe) {
        properties.setProperty("accessToken", accessToken != null ? accessToken : "");
        properties.setProperty("refreshToken", refreshToken != null ? refreshToken : "");
        properties.setProperty("userId", userId != null ? String.valueOf(userId) : "");
        properties.setProperty("username", username != null ? username : "");
        properties.setProperty("rememberMe", String.valueOf(rememberMe));
        save();
    }

    public static synchronized void saveAccessToken(String accessToken) {
        properties.setProperty("accessToken", accessToken != null ? accessToken : "");
        save();
    }

    public static synchronized void clearSession() {
        properties.remove("accessToken");
        properties.remove("refreshToken");
        properties.remove("userId");
        properties.remove("username");
        properties.remove("rememberMe");
        properties.remove("lastSyncTimestamp");
        save();
    }

    public static synchronized String getAccessToken() {
        return properties.getProperty("accessToken");
    }

    public static synchronized String getRefreshToken() {
        return properties.getProperty("refreshToken");
    }

    public static synchronized Long getUserId() {
        String idStr = properties.getProperty("userId");
        if (idStr == null || idStr.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static synchronized String getUsername() {
        return properties.getProperty("username");
    }

    public static synchronized boolean isRememberMe() {
        return Boolean.parseBoolean(properties.getProperty("rememberMe", "false"));
    }

    public static synchronized void setLastSyncTimestamp(long timestamp) {
        properties.setProperty("lastSyncTimestamp", String.valueOf(timestamp));
        save();
    }

    public static synchronized long getLastSyncTimestamp() {
        String timeStr = properties.getProperty("lastSyncTimestamp");
        if (timeStr == null || timeStr.isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(timeStr);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public static synchronized boolean hasToken() {
        String token = getAccessToken();
        return token != null && !token.trim().isEmpty();
    }

    public static synchronized void setServerUrl(String serverUrl) {
        if (serverUrl != null) {
            serverUrl = serverUrl.trim();
            if (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }
        }
        properties.setProperty("serverUrl", serverUrl != null && !serverUrl.isEmpty() ? serverUrl : "http://115.99.50.73:50000");
        save();
    }

    public static synchronized String getServerUrl() {
        return properties.getProperty("serverUrl", "http://115.99.50.73:50000");
    }
}
