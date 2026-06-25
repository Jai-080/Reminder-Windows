package com.reminder.desktop.config;

public final class ServerConfig {
    private ServerConfig() {}

    public static final String SERVER_URL = "http://your-server-address:50000/";

    public static final String WS_BASE_URL = SERVER_URL.replaceFirst("^http", "ws");

    public static String getServerHost() {
        return SERVER_URL
                .replaceFirst("^https?://", "")
                .replaceAll("/$", "");
    }
}
