package com.reminder.desktop.dto;

public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String deviceName;
    private String platform;

    public RegisterRequest() {
    }

    public RegisterRequest(String username, String email, String password, String deviceName, String platform) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.deviceName = deviceName;
        this.platform = platform;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
