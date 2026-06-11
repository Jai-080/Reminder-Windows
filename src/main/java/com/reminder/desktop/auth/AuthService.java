package com.reminder.desktop.auth;

public interface AuthService {
    boolean login(String email, String password, boolean rememberMe) throws Exception;
    boolean register(String username, String email, String password) throws Exception;
    void logout();
    boolean refreshSession() throws Exception;
}
