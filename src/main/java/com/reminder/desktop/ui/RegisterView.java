package com.reminder.desktop.ui;

import com.reminder.desktop.MainApplication;
import com.reminder.desktop.auth.TokenStorage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class RegisterView extends VBox {
    private final MainApplication app;
    private final RegisterController controller;

    public RegisterView(MainApplication app) {
        this.app = app;
        this.controller = new RegisterController(app);

        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(24));
        this.setSpacing(24);

        ThemeManager.registerRoot(this);

        // Container Card
        VBox card = new VBox();
        card.getStyleClass().add("card");
        card.setMaxWidth(380);
        card.setPadding(new Insets(32, 24, 32, 24));
        card.setSpacing(16);
        card.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Create Account");
        title.getStyleClass().add("title-label");

        Label subtitle = new Label("Join the productivity network. Sync notes, reminders, and payments.");
        subtitle.getStyleClass().add("subtitle-label");
        subtitle.setWrapText(true);

        VBox userBox = new VBox(6);
        Label userLabel = new Label("Username");
        TextField userField = new TextField();
        userField.setPromptText("johndoe");
        userBox.getChildren().addAll(userLabel, userField);

        VBox emailBox = new VBox(6);
        Label emailLabel = new Label("Email Address");
        TextField emailField = new TextField();
        emailField.setPromptText("name@example.com");
        emailBox.getChildren().addAll(emailLabel, emailField);

        VBox passBox = new VBox(6);
        Label passLabel = new Label("Password");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Minimum 8 characters");
        passBox.getChildren().addAll(passLabel, passField);

        VBox urlBox = new VBox(6);
        Label urlLabel = new Label("Base Server URL");
        TextField urlField = new TextField();
        urlField.setPromptText("http://115.99.50.73:50000");
        urlField.setText(TokenStorage.getServerUrl());
        urlBox.getChildren().addAll(urlLabel, urlField);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: -color-danger; -fx-font-weight: bold;");
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);

        Button signUpBtn = new Button("Sign Up");
        signUpBtn.getStyleClass().add("button-accent");
        signUpBtn.setMaxWidth(Double.MAX_VALUE);

        Hyperlink loginLink = new Hyperlink("Already have an account? Sign in");
        loginLink.setStyle("-fx-text-fill: -color-accent;");
        loginLink.setOnAction(e -> app.showLogin());

        card.getChildren().addAll(
                title, subtitle,
                userBox, emailBox, passBox, urlBox,
                errorLabel,
                signUpBtn,
                loginLink
        );

        this.getChildren().add(card);

        // Action Trigger
        signUpBtn.setOnAction(e -> controller.handleRegister(
                userField,
                emailField,
                passField,
                urlField,
                errorLabel,
                signUpBtn
        ));
    }
}
