package com.reminder.desktop.ui;

import com.reminder.desktop.auth.AuthService;
import com.reminder.desktop.auth.AuthServiceImpl;
import com.reminder.desktop.auth.TokenStorage;
import com.reminder.desktop.MainApplication;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

public class RegisterController {
    private final MainApplication app;
    private final AuthService authService;

    public RegisterController(MainApplication app) {
        this.app = app;
        this.authService = new AuthServiceImpl();
    }

    public void handleRegister(TextField usernameField, TextField emailField, TextField passField, Label errorLabel, Button signUpBtn) {
        String username = usernameField != null && usernameField.getText() != null ? usernameField.getText().trim() : "";
        String email = emailField != null && emailField.getText() != null ? emailField.getText().trim() : "";
        String password = passField != null && passField.getText() != null ? passField.getText() : "";

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("All fields are required.");
            errorLabel.setVisible(true);
            return;
        }

        if (password.length() < 8) {
            errorLabel.setText("Password must be at least 8 characters.");
            errorLabel.setVisible(true);
            return;
        }

        signUpBtn.setText("Creating Account...");
        signUpBtn.setDisable(true);
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                boolean success = authService.register(username, email, password);
                Platform.runLater(() -> {
                    if (success) {
                        app.showDashboard();
                    } else {
                        errorLabel.setText("Registration failed. Please try again.");
                        errorLabel.setVisible(true);
                        signUpBtn.setText("Sign Up");
                        signUpBtn.setDisable(false);
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
                    signUpBtn.setText("Sign Up");
                    signUpBtn.setDisable(false);
                });
            }
        }).start();
    }
}
