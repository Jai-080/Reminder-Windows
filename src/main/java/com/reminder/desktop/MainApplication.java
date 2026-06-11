package com.reminder.desktop;

import com.reminder.desktop.auth.TokenStorage;
import com.reminder.desktop.notifications.ReminderScheduler;
import com.reminder.desktop.ui.LoginView;
import com.reminder.desktop.ui.MainLayout;
import com.reminder.desktop.ui.RegisterView;
import com.reminder.desktop.ui.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class MainApplication extends Application {
    private static MainApplication instance;
    private Stage primaryStage;

    public static MainApplication getInstance() {
        return instance;
    }

    public static void handleSessionExpired(String message) {
        javafx.application.Platform.runLater(() -> {
            if (instance != null) {
                // Clear session
                com.reminder.desktop.auth.TokenStorage.clearSession();
                // Cancel all scheduler checks
                com.reminder.desktop.notifications.ReminderScheduler.getInstance().cancelAll();
                // Show notification warning
                com.reminder.desktop.notifications.NotificationManager.getInstance()
                        .showWarningNotification("Session Expired", message);
                // Redirect back to Login Screen
                instance.showLogin(message);
            }
        });
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        this.primaryStage = stage;
        primaryStage.setTitle("Reminder Desktop");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // Check if there is an active session stored
        if (TokenStorage.hasToken()) {
            showDashboard();
        } else {
            showLogin();
        }
        
        primaryStage.show();
    }

    public void showLogin() {
        showLogin(null);
    }

    public void showLogin(String initialError) {
        LoginView loginView = new LoginView(this, initialError);
        Scene scene = new Scene(loginView, 900, 600);
        ThemeManager.registerRoot(loginView);
        primaryStage.setScene(scene);
    }

    public void showRegister() {
        RegisterView registerView = new RegisterView(this);
        Scene scene = new Scene(registerView, 900, 600);
        ThemeManager.registerRoot(registerView);
        primaryStage.setScene(scene);
    }

    public void showDashboard() {
        // Initialize alarm task scheduler on successful user login/restore
        ReminderScheduler.getInstance().initialize();

        MainLayout mainLayout = new MainLayout(this);
        Scene scene = new Scene(mainLayout, 1050, 680);
        ThemeManager.registerRoot(mainLayout);
        primaryStage.setScene(scene);
    }

    @Override
    public void stop() {
        // Clean up scheduler background tasks on app exit
        ReminderScheduler.getInstance().cancelAll();
        System.out.println("Reminder Desktop application stopped successfully.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
