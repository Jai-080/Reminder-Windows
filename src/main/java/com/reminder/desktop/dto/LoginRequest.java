package com.reminder.desktop.dto;

public class LoginRequest {
    private String email;
    private String password;
    private String deviceName;
    private String platform;

    public LoginRequest() {
    }

    public LoginRequest(String email, String password, String deviceName, String platform) {
        this.email = email;
        this.password = password;
        this.deviceName = deviceName;
        this.platform = platform;
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
