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
        String email = emailField.getText();
        String password = passField.getText();

        if (email.trim().isEmpty() || password.trim().isEmpty()) {
            errorLabel.setText("Email and password are required.");
            errorLabel.setVisible(true);
            return;
        }

        signInBtn.setDisable(true);
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                boolean success = authService.login(email, password, true);
                Platform.runLater(() -> {
                    if (success) {
                        app.showDashboard();
                    } else {
                        errorLabel.setText("Authentication failed.");
                        errorLabel.setVisible(true);
                        signInBtn.setDisable(false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    errorLabel.setText(UIUtils.sanitizeError(ex.getMessage()));
                    errorLabel.setVisible(true);
                    signInBtn.setDisable(false);
                });
            }
        }).start();
    }
}
