package com.reminder.desktop.ui;

import com.reminder.desktop.auth.AuthService;
import com.reminder.desktop.auth.AuthServiceImpl;
import com.reminder.desktop.auth.TokenStorage;
import com.reminder.desktop.MainApplication;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

public class LoginController {
    private final MainApplication app;
    private final AuthService authService;

    public LoginController(MainApplication app) {
        this.app = app;
        this.authService = new AuthServiceImpl();
    }

    public void handleLogin(TextField emailField, TextField passField, Label errorLabel, Button signInBtn) {
        String email = emailField != null && emailField.getText() != null ? emailField.getText().trim() : "";
        String password = passField != null && passField.getText() != null ? passField.getText().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Email and password are required.");
            errorLabel.setVisible(true);
            return;
        }

        signInBtn.setText("Signing In...");
        signInBtn.setDisable(true);
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                boolean success = authService.login(email, password, true);
                Platform.runLater(() -> {
                    if (success) {
                        app.showDashboard();
                    } else {
                        errorLabel.setText("Authentication failed. Please verify your credentials.");
                        errorLabel.setVisible(true);
                        signInBtn.setText("Sign In");
                        signInBtn.setDisable(false);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    String rawMsg = ex.getMessage();
                    if (rawMsg == null || rawMsg.trim().isEmpty()) {
                        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                            rawMsg = ex.getCause().getMessage();
                        } else {
                            rawMsg = "Connection failed: " + ex.getClass().getSimpleName();
                        }
                    }
                    String displayMsg = UIUtils.sanitizeError(rawMsg);
                    if (displayMsg == null || displayMsg.trim().isEmpty()) {
                        displayMsg = "Unable to connect to server. Please check your internet connection.";
                    }
                    errorLabel.setText(displayMsg);
                    errorLabel.setVisible(true);
                    signInBtn.setText("Sign In");
                    signInBtn.setDisable(false);
                });
            }
        }).start();
    }
}
