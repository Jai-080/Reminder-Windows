package com.reminder.desktop.ui;

import com.reminder.desktop.MainApplication;
import com.reminder.desktop.auth.TokenStorage;
import com.reminder.desktop.auth.AuthServiceImpl;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class Sidebar extends VBox {
    private final MainLayout layout;
    private final MainApplication app;
    private Button dashBtn;
    private Button notesBtn;
    private Button remindersBtn;
    private Button paymentsBtn;

    public Sidebar(MainLayout layout, MainApplication app) {
        this.layout = layout;
        this.app = app;

        this.getStyleClass().add("sidebar");
        this.setPrefWidth(220);
        this.setSpacing(8);

        // Header Title
        Label appName = new Label("Reminder");
        appName.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: -color-accent;");
        Label appSub = new Label(" ");
        appSub.getStyleClass().add("subtitle-label");
        VBox header = new VBox(2, appName, appSub);
        header.setPadding(new Insets(0, 0, 24, 8));

        // Navigation Buttons
        dashBtn = createNavButton("Dashboard");
        notesBtn = createNavButton("Quick Notes");
        remindersBtn = createNavButton("Reminders");
        paymentsBtn = createNavButton("Monthly Payments");

        dashBtn.setOnAction(e -> layout.showView("Dashboard"));
        notesBtn.setOnAction(e -> layout.showView("Notes"));
        remindersBtn.setOnAction(e -> layout.showView("Reminders"));
        paymentsBtn.setOnAction(e -> layout.showView("Payments"));

        VBox navBox = new VBox(4, dashBtn, notesBtn, remindersBtn, paymentsBtn);

        // Spacer to push profile/logout to the bottom
        VBox spacer = new VBox();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // User info & theme toggle
        String usernameVal = TokenStorage.getUsername();
        Label userLabel = new Label("Hello, " + (usernameVal != null ? usernameVal : "User") + "!");
        userLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 0 4 16;");

        Label modeLabel = new Label("Dark Mode");
        modeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-text;");
        javafx.scene.layout.Region space = new javafx.scene.layout.Region();
        HBox.setHgrow(space, Priority.ALWAYS);

        CheckBox themeToggle = new CheckBox();
        themeToggle.getStyleClass().add("toggle-switch");
        themeToggle.setSelected(ThemeManager.isDarkMode());
        themeToggle.setOnAction(e -> ThemeManager.setDarkMode(themeToggle.isSelected()));

        HBox themeBox = new HBox(10, modeLabel, space, themeToggle);
        themeBox.setAlignment(Pos.CENTER_LEFT);
        themeBox.setPadding(new Insets(6, 16, 6, 16));

        Button logoutBtn = new Button("Log Out");
        logoutBtn.getStyleClass().add("sidebar-button");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle("-fx-text-fill: -color-danger;");
        logoutBtn.setOnAction(e -> {
            new AuthServiceImpl().logout();
            app.showLogin();
        });

        VBox footer = new VBox(6, userLabel, themeBox, logoutBtn);
        footer.setPadding(new Insets(24, 0, 0, 0));

        this.getChildren().addAll(header, navBox, spacer, footer);
        setActiveButton("Dashboard");
    }

    private Button createNavButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    public void setActiveButton(String viewName) {
        dashBtn.getStyleClass().remove("active");
        notesBtn.getStyleClass().remove("active");
        remindersBtn.getStyleClass().remove("active");
        paymentsBtn.getStyleClass().remove("active");

        switch (viewName) {
            case "Dashboard":
                dashBtn.getStyleClass().add("active");
                break;
            case "Notes":
                notesBtn.getStyleClass().add("active");
                break;
            case "Reminders":
                remindersBtn.getStyleClass().add("active");
                break;
            case "Payments":
                paymentsBtn.getStyleClass().add("active");
                break;
        }
    }
}
