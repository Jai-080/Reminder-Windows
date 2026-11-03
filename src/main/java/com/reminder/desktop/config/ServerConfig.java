package com.reminder.desktop.config;

public final class ServerConfig {
    private ServerConfig() {}

    public static final String PRODUCTION_SERVER_URL = "http://jaireminderapp.duckdns.org:50000/";
    public static final String DEV_SERVER_URL = "http://localhost:8088/";

    public static boolean isDevMode() {
        String prop = System.getProperty("reminder.server.url");
        if (prop != null && !prop.trim().isEmpty()) {
            return true;
        }
        String env = System.getenv("REMINDER_ENV");
        if ("dev".equalsIgnoreCase(env) || "development".equalsIgnoreCase(env)) {
            return true;
        }
        String envUrl = System.getenv("REMINDER_SERVER_URL");
        return envUrl != null && !envUrl.trim().isEmpty();
    }

    public static String getEffectiveServerUrl() {
        String prop = System.getProperty("reminder.server.url");
        if (prop != null && !prop.trim().isEmpty()) {
            return prop.trim().endsWith("/") ? prop.trim() : prop.trim() + "/";
        }
        String env = System.getenv("REMINDER_ENV");
        if ("dev".equalsIgnoreCase(env) || "development".equalsIgnoreCase(env)) {
            return DEV_SERVER_URL;
        }
        String envUrl = System.getenv("REMINDER_SERVER_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            return envUrl.trim().endsWith("/") ? envUrl.trim() : envUrl.trim() + "/";
        }
        return PRODUCTION_SERVER_URL;
    }

    public static final String SERVER_URL = getEffectiveServerUrl();

    public static final String WS_BASE_URL = SERVER_URL.replaceFirst("^http", "ws");

    public static String getServerHost() {
        return getEffectiveServerUrl()
                .replaceFirst("^https?://", "")
                .replaceAll("/$", "");
    }
}
