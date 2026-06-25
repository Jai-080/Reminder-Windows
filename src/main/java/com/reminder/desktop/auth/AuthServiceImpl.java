package com.reminder.desktop.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reminder.desktop.dto.LoginRequest;
import com.reminder.desktop.dto.LoginResponse;
import com.reminder.desktop.dto.RefreshTokenRequest;
import com.reminder.desktop.dto.RegisterRequest;
import com.reminder.desktop.dto.RegisterResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AuthServiceImpl implements AuthService {
    private String getBaseUrl() {
        return TokenStorage.getServerUrl();
    }
    private final HttpClient client;
    private final ObjectMapper mapper;

    public AuthServiceImpl() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public boolean login(String email, String password, boolean rememberMe) throws Exception {
        String computerName = System.getenv("COMPUTERNAME");
        if (computerName == null || computerName.trim().isEmpty()) {
            computerName = "Windows-PC";
        }
        LoginRequest req = new LoginRequest(email, password, computerName, "windows");
        String jsonPayload = mapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            LoginResponse resp = mapper.readValue(response.body(), LoginResponse.class);
            TokenStorage.saveSession(
                    resp.getAccessToken(),
                    resp.getRefreshToken(),
                    resp.getUserId(),
                    resp.getUsername(),
                    rememberMe
            );
            return true;
        } else {
            String errorMsg = "Login failed: " + response.statusCode();
            try {
                // Try parsing error message if available
                var errorNode = mapper.readTree(response.body());
                if (errorNode.has("message")) {
                    errorMsg = errorNode.get("message").asText();
                }
            } catch (Exception ignored) {}
            throw new Exception(errorMsg);
        }
    }

    @Override
    public boolean register(String username, String email, String password) throws Exception {
        String computerName = System.getenv("COMPUTERNAME");
        if (computerName == null || computerName.trim().isEmpty()) {
            computerName = "Windows-PC";
        }
        RegisterRequest req = new RegisterRequest(username, email, password, computerName, "windows");
        String jsonPayload = mapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            RegisterResponse resp = mapper.readValue(response.body(), RegisterResponse.class);
            TokenStorage.saveSession(
                    resp.getAccessToken(),
                    resp.getRefreshToken(),
                    resp.getUserId(),
                    resp.getUsername(),
                    true // Always remember on register
            );
            return true;
        } else {
            String errorMsg = "Registration failed: " + response.statusCode();
            try {
                var errorNode = mapper.readTree(response.body());
                if (errorNode.has("message")) {
                    errorMsg = errorNode.get("message").asText();
                }
            } catch (Exception ignored) {}
            throw new Exception(errorMsg);
        }
    }

    @Override
    public void logout() {
        String refreshToken = TokenStorage.getRefreshToken();
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                RefreshTokenRequest req = new RefreshTokenRequest(refreshToken);
                String jsonPayload = mapper.writeValueAsString(req);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(getBaseUrl() + "/api/auth/logout"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                // Fire and forget / best effort logout call
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                System.err.println("Error triggering server logout: " + e.getMessage());
            }
        }
        TokenStorage.clearSession();
        com.reminder.desktop.sync.WebSocketManager.getInstance().disconnect();
        com.reminder.desktop.sync.SyncService.getInstance().stopPeriodicSync();
    }

    @Override
    public boolean refreshSession() throws Exception {
        String refreshToken = TokenStorage.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            return false;
        }

        RefreshTokenRequest req = new RefreshTokenRequest(refreshToken);
        String jsonPayload = mapper.writeValueAsString(req);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() + "/api/auth/refresh"))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            LoginResponse resp = mapper.readValue(response.body(), LoginResponse.class);
            TokenStorage.saveSession(
                    resp.getAccessToken(),
                    resp.getRefreshToken(),
                    resp.getUserId(),
                    resp.getUsername(),
                    TokenStorage.isRememberMe()
            );
            return true;
        } else {
            TokenStorage.clearSession();
            return false;
        }
    }
}
