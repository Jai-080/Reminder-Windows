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
import javafx.scene.shape.SVGPath;

public class Sidebar extends VBox {
    private final MainLayout layout;
    private final MainApplication app;
    private final Button dashBtn;
    private final Button notesBtn;
    private final Button remindersBtn;
    private final Button paymentsBtn;

    // Clean Material Design Vector Icons
    private static final String ICON_HOME = "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z";
    private static final String ICON_NOTES = "M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z";
    private static final String ICON_REMINDERS = "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v6l-2 2v1h16v-1l-2-2z";
    private static final String ICON_PAYMENTS = "M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.11 0 2-.89 2-2V6c0-1.11-.89-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2z";

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

        // --- 2. NAVIGATION BUTTONS (With Clean Larger SVG Icons) ---
        dashBtn = createNavButton("Dashboard", ICON_HOME);
        notesBtn = createNavButton("Quick Notes", ICON_NOTES);
        remindersBtn = createNavButton("Reminders", ICON_REMINDERS);
        paymentsBtn = createNavButton("Payments", ICON_PAYMENTS);

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

        // Exact Animated Theme Toggle Switch Row (Only one near logout button)
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

    private Button createNavButton(String text, String svgPath) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        SVGPath icon = new SVGPath();
        icon.setContent(svgPath);
        icon.getStyleClass().add("sidebar-icon");

        btn.setGraphic(icon);
        btn.setGraphicTextGap(14);
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
