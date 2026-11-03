package com.reminder.desktop.ui;

import com.reminder.desktop.MainApplication;
import com.reminder.desktop.auth.TokenStorage;
import com.reminder.desktop.auth.AuthServiceImpl;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class Sidebar extends VBox {
    private final MainLayout layout;
    private final MainApplication app;
    private final Button dashBtn;
    private final Button notesBtn;
    private final Button remindersBtn;
    private final Button paymentsBtn;

    public Sidebar(MainLayout layout, MainApplication app) {
        this.layout = layout;
        this.app = app;

        this.getStyleClass().add("sidebar");
        this.setPrefWidth(240);
        this.setMinWidth(240);
        this.setMaxWidth(240);
        this.setSpacing(6);

        // --- 1. HEADER SECTION ---
        HBox brandRow = new HBox(8);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        Label appName = new Label("Reminder");
        appName.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: -color-text;");

        Label versionBadge = new Label("2.1");
        versionBadge.getStyleClass().addAll("badge-pill", "badge-pending");
        versionBadge.setStyle("-fx-font-size: 10px; -fx-padding: 1px 7px;");

        brandRow.getChildren().addAll(appName, versionBadge);

        Label appSub = new Label("Desktop Suite");
        appSub.getStyleClass().add("subtitle-label");
        appSub.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-secondary;");

        VBox header = new VBox(2, brandRow, appSub);
        header.setPadding(new Insets(4, 8, 20, 8));

        // --- 2. NAVIGATION BUTTONS ---
        dashBtn = createNavButton("🏠  Dashboard");
        notesBtn = createNavButton("📝  Quick Notes");
        remindersBtn = createNavButton("🔔  Reminders");
        paymentsBtn = createNavButton("💳  Payments");

        dashBtn.setOnAction(e -> layout.showView("Dashboard"));
        notesBtn.setOnAction(e -> layout.showView("Notes"));
        remindersBtn.setOnAction(e -> layout.showView("Reminders"));
        paymentsBtn.setOnAction(e -> layout.showView("Payments"));

        VBox navBox = new VBox(6, dashBtn, notesBtn, remindersBtn, paymentsBtn);

        // Spacer to push profile/controls to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // --- 3. USER PROFILE & THEME CONTROLS CARD ---
        VBox profileCard = new VBox(10);
        profileCard.getStyleClass().add("profile-card");

        // User Avatar + Info
        String usernameVal = TokenStorage.getUsername();
        String displayName = (usernameVal != null && !usernameVal.isEmpty()) ? usernameVal : "User";
        String initial = displayName.substring(0, 1).toUpperCase();

        StackPane avatar = new StackPane();
        avatar.setPrefSize(34, 34);
        avatar.setMinSize(34, 34);
        avatar.setMaxSize(34, 34);
        avatar.getStyleClass().add("avatar-circle");
        Label initialLabel = new Label(initial);
        initialLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: -color-on-accent;");
        avatar.getChildren().add(initialLabel);

        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: -color-text;");
        Label accountType = new Label("Personal Account");
        accountType.setStyle("-fx-font-size: 11px; -fx-text-fill: -color-text-secondary;");
        VBox userTextBox = new VBox(1, nameLabel, accountType);
        HBox.setHgrow(userTextBox, Priority.ALWAYS);

        HBox userRow = new HBox(10, avatar, userTextBox);
        userRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();
        sep.setStyle("-fx-opacity: 0.4;");

        // Exact Animated Theme Toggle Switch Row
        Label themeLabel = new Label("Theme");
        themeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -color-text;");
        Region themeSpace = new Region();
        HBox.setHgrow(themeSpace, Priority.ALWAYS);

        ThemeToggleSwitch themeSwitch = new ThemeToggleSwitch();

        HBox themeBox = new HBox(8, themeLabel, themeSpace, themeSwitch);
        themeBox.setAlignment(Pos.CENTER_LEFT);
        themeBox.setPadding(new Insets(2, 0, 2, 0));

        // Log out button
        Button logoutBtn = new Button("Log Out");
        logoutBtn.getStyleClass().add("button-danger");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setAlignment(Pos.CENTER);
        logoutBtn.setOnAction(e -> {
            new AuthServiceImpl().logout();
            app.showLogin();
        });

        profileCard.getChildren().addAll(userRow, sep, themeBox, logoutBtn);

        this.getChildren().addAll(header, navBox, spacer, profileCard);
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
