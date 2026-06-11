package com.reminder.desktop.ui;

import com.reminder.desktop.MainApplication;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class LoginView extends VBox {
    private final MainApplication app;
    private final LoginController controller;

    public LoginView(MainApplication app) {
        this(app, null);
    }

    public LoginView(MainApplication app, String initialError) {
        this.app = app;
        this.controller = new LoginController(app);

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

        Label title = new Label("Sign In");
        title.getStyleClass().add("title-label");

        Label subtitle = new Label("Welcome back! Access your reminders ecosystem.");
        subtitle.getStyleClass().add("subtitle-label");
        subtitle.setWrapText(true);

        VBox emailBox = new VBox(6);
        Label emailLabel = new Label("Email Address");
        TextField emailField = new TextField();
        emailField.setPromptText("name@example.com");
        emailBox.getChildren().addAll(emailLabel, emailField);

        VBox passBox = new VBox(6);
        Label passLabel = new Label("Password");
        PasswordField passField = new PasswordField();
        passField.setPromptText("••••••••");
        passBox.getChildren().addAll(passLabel, passField);

        CheckBox rememberMe = new CheckBox("Remember Login");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: -color-danger; -fx-font-weight: bold;");
        errorLabel.setWrapText(true);
        if (initialError != null && !initialError.isEmpty()) {
            errorLabel.setText(initialError);
            errorLabel.setVisible(true);
        } else {
            errorLabel.setVisible(false);
        }

        Button signInBtn = new Button("Sign In");
        signInBtn.getStyleClass().add("button-accent");
        signInBtn.setMaxWidth(Double.MAX_VALUE);

        Hyperlink createAccountLink = new Hyperlink("Don't have an account? Sign up");
        createAccountLink.setStyle("-fx-text-fill: -color-accent;");
        createAccountLink.setOnAction(e -> app.showRegister());

        card.getChildren().addAll(
                title, subtitle,
                emailBox, passBox,
                rememberMe,
                errorLabel,
                signInBtn,
                createAccountLink
        );

        this.getChildren().add(card);

        // Action Trigger
        signInBtn.setOnAction(e -> controller.handleLogin(
                emailField,
                passField,
                rememberMe,
                errorLabel,
                signInBtn
        ));
    }
}
