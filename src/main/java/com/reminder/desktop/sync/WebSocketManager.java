package com.reminder.desktop.sync;

import com.reminder.desktop.auth.TokenStorage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Random;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebSocketManager {
    private static WebSocketManager instance;

    private final ScheduledExecutorService scheduler;
    private final Random random;

    private WebSocket webSocket;
    private boolean isConnecting = false;
    private boolean isConnected = false;
    private boolean userWantsConnection = false;
    private int reconnectAttempts = 0;
    
    private final StringBuilder messageBuffer = new StringBuilder();

    private WebSocketManager() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WebSocketScheduler");
            t.setDaemon(true);
            return t;
        });
        this.random = new Random();
    }

    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    public synchronized void connect() {
        if (!TokenStorage.hasToken()) {
            System.out.println("WebSocketManager: Cannot connect WebSocket, user not logged in.");
            return;
        }

        userWantsConnection = true;

        if (isConnected || isConnecting) {
            System.out.println("WebSocketManager: Already connected or connecting.");
            return;
        }

        isConnecting = true;
        reconnectAttempts = 0;
        executeConnect();
    }

    private synchronized void executeConnect() {
        String token = TokenStorage.getAccessToken();
        if (token == null) {
            System.err.println("WebSocketManager: Cannot connect, access token is null");
            isConnecting = false;
            return;
        }

        // Format WebSocket URL from Base URL
        String baseUrl = TokenStorage.getServerUrl();
        String wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://");
        if (!wsUrl.endsWith("/")) {
            wsUrl += "/";
        }
        wsUrl += "ws?token=" + token;
        System.out.println("WebSocketManager: Connecting to " + wsUrl);

        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(wsUrl), new WebSocketListenerImpl())
                .whenComplete((ws, throwable) -> {
                    if (throwable != null) {
                        System.err.println("WebSocketManager: Connection handshake failed: " + throwable.getMessage());
                        synchronized (WebSocketManager.this) {
                            isConnected = false;
                            isConnecting = false;
                        }
                        triggerReconnectIfNeeded();
                    }
                });
    }

    private class WebSocketListenerImpl implements WebSocket.Listener {
        @Override
        public void onOpen(WebSocket ws) {
            synchronized (WebSocketManager.this) {
                webSocket = ws;
            }
            String connectFrame = "CONNECT\n" +
                    "accept-version:1.1,1.2\n" +
                    "heart-beat:0,0\n" +
                    "Authorization:Bearer " + TokenStorage.getAccessToken() + "\n" +
                    "\n" +
                    "\u0000";
            System.out.println("WebSocketManager: STOMP Frame Outgoing [CONNECT]:\n" + connectFrame);
            ws.sendText(connectFrame, true);
            ws.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            messageBuffer.append(data);
            if (last) {
                String frame = messageBuffer.toString();
                messageBuffer.setLength(0);
                handleStompFrame(frame);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            System.out.println("WebSocketManager: Connection closed: " + reason + " (" + statusCode + ")");
            synchronized (WebSocketManager.this) {
                isConnected = false;
                isConnecting = false;
                if (webSocket == ws) {
                    webSocket = null;
                }
            }
            triggerReconnectIfNeeded();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            System.err.println("WebSocketManager: WebSocket error: " + error.getMessage());
            synchronized (WebSocketManager.this) {
                isConnected = false;
                isConnecting = false;
                if (webSocket == ws) {
                    webSocket = null;
                }
            }
            triggerReconnectIfNeeded();
        }
    }

    private synchronized void handleStompFrame(String frameText) {
        if (frameText == null || frameText.isEmpty()) return;

        String[] lines = frameText.split("\n");
        if (lines.length == 0) return;

        String command = lines[0].trim();
        System.out.println("WebSocketManager: Received STOMP command " + command);

        if ("CONNECTED".equals(command)) {
            System.out.println("WebSocketManager: STOMP Frame Incoming [CONNECTED]:\n" + frameText);
            synchronized (this) {
                isConnected = true;
                isConnecting = false;
                reconnectAttempts = 0;
            }
            String subscribeFrame = "SUBSCRIBE\n" +
                    "id:sub-0\n" +
                    "destination:/user/topic/sync\n" +
                    "\n" +
                    "\u0000";
            System.out.println("WebSocketManager: STOMP Frame Outgoing [SUBSCRIBE]:\n" + subscribeFrame);
            if (webSocket != null) {
                webSocket.sendText(subscribeFrame, true);
            }

            // Phase 13 Startup Recovery: trigger immediate sync after CONNECTED
            System.out.println("WebSocketManager: WebSocket connected successfully. Triggering recovery sync.");
            SyncService.getInstance().syncAll();

        } else if ("MESSAGE".equals(command)) {
            System.out.println("MESSAGE received");
            System.out.println("WebSocketManager: STOMP Frame Incoming [MESSAGE]:\n" + frameText);
            int bodyStartIndex = -1;
            for (int i = 1; i < lines.length; i++) {
                if (lines[i].trim().isEmpty()) {
                    bodyStartIndex = i + 1;
                    break;
                }
            }

            if (bodyStartIndex != -1 && bodyStartIndex < lines.length) {
                StringBuilder bodyBuilder = new StringBuilder();
                for (int i = bodyStartIndex; i < lines.length; i++) {
                    bodyBuilder.append(lines[i]);
                }
                String body = bodyBuilder.toString().replace("\u0000", "").trim();
                System.out.println("WebSocketManager: Received WebSocket SyncEvent payload: " + body);

                // Trigger the existing sync engine
                System.out.println("WebSocketManager: Sync event received. Triggering Sync Service.");
                SyncService.getInstance().syncAll();
            }
        } else if ("ERROR".equals(command)) {
            System.err.println("WebSocketManager: STOMP Frame Incoming [ERROR]:\n" + frameText);
        }
    }

    public synchronized void disconnect() {
        userWantsConnection = false;
        if (webSocket != null) {
            String disconnectFrame = "DISCONNECT\n\n\u0000";
            System.out.println("WebSocketManager: STOMP Frame Outgoing [DISCONNECT]:\n" + disconnectFrame);
            try {
                webSocket.sendText(disconnectFrame, true);
            } catch (Exception ignored) {}
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Logout");
            webSocket = null;
        }
        isConnected = false;
        isConnecting = false;
    }

    private synchronized void triggerReconnectIfNeeded() {
        if (!userWantsConnection || !TokenStorage.hasToken()) {
            return;
        }

        reconnectAttempts++;
        long delaySec;
        if (reconnectAttempts == 1) delaySec = 5;
        else if (reconnectAttempts == 2) delaySec = 10;
        else if (reconnectAttempts == 3) delaySec = 20;
        else delaySec = 60;

        // Add randomized jitter of ±1-2 seconds
        long jitter = random.nextInt(3) - 1; // -1, 0, or 1 seconds
        long finalDelayMs = Math.max(1000, (delaySec + jitter) * 1000);

        System.out.println("WebSocketManager: Scheduling reconnect attempt #" + reconnectAttempts + " in " + finalDelayMs + " ms.");
        scheduler.schedule(() -> {
            synchronized (WebSocketManager.this) {
                if (userWantsConnection && TokenStorage.hasToken() && !isConnected && !isConnecting) {
                    isConnecting = true;
                    executeConnect();
                }
            }
        }, finalDelayMs, TimeUnit.MILLISECONDS);
    }

    public synchronized boolean isConnected() {
        return isConnected;
    }
}
