package com.reminder.desktop.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reminder.desktop.auth.AuthService;
import com.reminder.desktop.auth.AuthServiceImpl;
import com.reminder.desktop.auth.TokenStorage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {
    private static ApiClient instance;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final AuthService authService;

    private ApiClient() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
        this.authService = new AuthServiceImpl();
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public <T> T get(String endpoint, Class<T> responseClass) throws Exception {
        return execute(endpoint, "GET", null, responseClass);
    }

    public <T> T post(String endpoint, Object body, Class<T> responseClass) throws Exception {
        return execute(endpoint, "POST", body, responseClass);
    }

    public <T> T put(String endpoint, Object body, Class<T> responseClass) throws Exception {
        return execute(endpoint, "PUT", body, responseClass);
    }

    public void delete(String endpoint) throws Exception {
        execute(endpoint, "DELETE", null, Void.class);
    }

    private <T> T execute(String endpoint, String method, Object body, Class<T> responseClass) throws Exception {
        return executeInternal(endpoint, method, body, responseClass, false);
    }

    private <T> T executeInternal(String endpoint, String method, Object body, Class<T> responseClass, boolean isRetry) throws Exception {
        String url = TokenStorage.getServerUrl() + endpoint;
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json");

        String token = TokenStorage.getAccessToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
        }

        if (body != null) {
            String json = mapper.writeValueAsString(body);
            builder.header("Content-Type", "application/json");
            if (method.equals("POST")) {
                builder.POST(HttpRequest.BodyPublishers.ofString(json));
            } else if (method.equals("PUT")) {
                builder.PUT(HttpRequest.BodyPublishers.ofString(json));
            } else {
                builder.method(method, HttpRequest.BodyPublishers.ofString(json));
            }
        } else {
            if (method.equals("GET")) {
                builder.GET();
            } else if (method.equals("DELETE")) {
                builder.DELETE();
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }
        }

        HttpRequest request = builder.build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new Exception("Network connection failure: " + e.getMessage(), e);
        }

        if (response.statusCode() == 401) {
            if (!isRetry) {
                System.out.println("Encountered 401 Unauthorized, attempting token refresh...");
                boolean refreshed = authService.refreshSession();
                if (refreshed) {
                    // Retry request once with the new token
                    return executeInternal(endpoint, method, body, responseClass, true);
                }
            }
            TokenStorage.clearSession();
            com.reminder.desktop.MainApplication.handleSessionExpired("Session expired. Please log in again.");
            throw new Exception("Session expired. Please log in again.");
        }

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            if (responseClass == Void.class || response.body() == null || response.body().trim().isEmpty()) {
                return null;
            }
            return mapper.readValue(response.body(), responseClass);
        } else {
            String errorMsg = "API call failed with status " + response.statusCode();
            try {
                var errorNode = mapper.readTree(response.body());
                if (errorNode.has("message")) {
                    errorMsg = errorNode.get("message").asText();
                }
            } catch (Exception ignored) {}
            throw new Exception(errorMsg);
        }
    }
}
