package com.reminder.desktop;

import com.reminder.desktop.auth.TokenStorage;
import com.reminder.desktop.notifications.ReminderScheduler;
import com.reminder.desktop.ui.LoginView;
import com.reminder.desktop.ui.MainLayout;
import com.reminder.desktop.ui.RegisterView;
import com.reminder.desktop.ui.ThemeManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
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
                // Disconnect WebSocket and stop scheduler
                com.reminder.desktop.sync.WebSocketManager.getInstance().disconnect();
                com.reminder.desktop.sync.SyncService.getInstance().stopPeriodicSync();
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
        javafx.application.Platform.setImplicitExit(false);

        // Detect if launched automatically by Windows startup
        boolean startHidden = getParameters().getRaw().contains("--startup");

        // Auto-update startup path if startup registration exists (handles app updates)
        if (WindowsStartupManager.isStartupEnabled()) {
            try {
                WindowsStartupManager.enableStartup();
            } catch (Exception e) {
                System.err.println("Failed to auto-update startup entry path: " + e.getMessage());
            }
        }

        // Initialize Tray Icon immediately on startup
        com.reminder.desktop.notifications.TrayManager.getInstance();

        primaryStage.setTitle("Reminder Desktop");
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/ic_launcher.png")));
        } catch (Exception e) {
            System.err.println("Could not load application window icon: " + e.getMessage());
        }
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);

        // Check if there is an active session stored
        if (TokenStorage.hasToken()) {
            showDashboard();
        } else {
            showLogin();
        }
        
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            primaryStage.hide();
        });

        // Show window only if NOT launched from Windows startup
        if (!startHidden) {
            primaryStage.show();
        }
    }

    public void showAndFocus() {
        if (primaryStage != null) {
            boolean maximized = primaryStage.isMaximized();
            if (primaryStage.isIconified()) {
                primaryStage.setIconified(false);
            }
            primaryStage.show();
            primaryStage.setMaximized(maximized);
            primaryStage.toFront();
            primaryStage.requestFocus();
        }
    }

    public void exitApplication() {
        javafx.application.Platform.runLater(() -> {
            com.reminder.desktop.notifications.TrayManager.getInstance().shutdown();
            javafx.application.Platform.exit();
        });
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

        // Phase 11: Connect WebSocket and Phase 13: Start periodic background sync
        com.reminder.desktop.sync.WebSocketManager.getInstance().connect();
        com.reminder.desktop.sync.SyncService.getInstance().startPeriodicSync();

        // Trigger background synchronization asynchronously
        com.reminder.desktop.sync.SyncService.getInstance().triggerSyncAsync(null);

        MainLayout mainLayout = new MainLayout(this);
        Scene scene = new Scene(mainLayout, 1050, 680);
        ThemeManager.registerRoot(mainLayout);
        primaryStage.setScene(scene);
    }

    @Override
    public void stop() {
        // Clean up scheduler background tasks on app exit
        com.reminder.desktop.sync.WebSocketManager.getInstance().disconnect();
        com.reminder.desktop.sync.SyncService.getInstance().stopPeriodicSync();
        ReminderScheduler.getInstance().cancelAll();
        
        // Release SingleInstanceManager resources
        SingleInstanceManager.shutdown();
        
        // Remove system tray icon
        com.reminder.desktop.notifications.TrayManager.getInstance().shutdown();
        
        System.out.println("Reminder Desktop application stopped successfully.");
        
        // Force JVM termination to clear any AWT/other non-daemon threads
        System.exit(0);
    }

    public static void main(String[] args) {
        if (!SingleInstanceManager.checkAndRegister()) {
            System.exit(0);
        }
        launch(args);
    }
}
