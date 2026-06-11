package com.reminder.desktop.ui;

import com.reminder.desktop.auth.AuthService;
import com.reminder.desktop.auth.AuthServiceImpl;
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
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passField.getText();

        if (username.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
            errorLabel.setText("All fields are required.");
            errorLabel.setVisible(true);
            return;
        }

        if (password.length() < 8) {
            errorLabel.setText("Password must be at least 8 characters.");
            errorLabel.setVisible(true);
            return;
        }

        signUpBtn.setDisable(true);
        errorLabel.setVisible(false);

        new Thread(() -> {
            try {
                boolean success = authService.register(username, email, password);
                Platform.runLater(() -> {
                    if (success) {
                        app.showDashboard();
                    } else {
                        errorLabel.setText("Registration failed.");
                        errorLabel.setVisible(true);
                        signUpBtn.setDisable(false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    errorLabel.setText(ex.getMessage());
                    errorLabel.setVisible(true);
                    signUpBtn.setDisable(false);
                });
            }
        }).start();
    }
}
