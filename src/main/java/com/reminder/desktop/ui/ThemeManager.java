package com.reminder.desktop.ui;

import com.reminder.desktop.auth.TokenStorage;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ThemeManager {

    public interface ThemeListener {
        void onThemeChanged(boolean isDarkMode);
    }

    private static boolean darkMode = TokenStorage.isDarkMode();
    private static final List<Parent> registeredRoots = new ArrayList<>();
    private static final List<ThemeListener> themeListeners = new CopyOnWriteArrayList<>();

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void toggleTheme() {
        setDarkMode(!darkMode);
    }

    public static void setDarkMode(boolean dark) {
        darkMode = dark;
        TokenStorage.saveDarkMode(dark);

        // Apply animated theme changes to all open roots
        Platform.runLater(() -> {
            for (Parent root : new ArrayList<>(registeredRoots)) {
                applyAnimatedTheme(root);
            }
            // Notify all registered theme listeners
            for (ThemeListener listener : themeListeners) {
                try {
                    listener.onThemeChanged(dark);
                } catch (Exception e) {
                    System.err.println("Error notifying ThemeListener: " + e.getMessage());
                }
            }
        });
    }

    public static void addListener(ThemeListener listener) {
        if (listener != null && !themeListeners.contains(listener)) {
            themeListeners.add(listener);
        }
    }

    public static void removeListener(ThemeListener listener) {
        if (listener != null) {
            themeListeners.remove(listener);
        }
    }

    public static void registerRoot(Parent root) {
        if (root == null) return;
        if (!registeredRoots.contains(root)) {
            registeredRoots.add(root);
        }
        applyCurrentTheme(root);
    }

    public static void unregisterRoot(Parent root) {
        registeredRoots.remove(root);
    }

    private static void applyAnimatedTheme(Parent root) {
        if (root == null) return;

        // Smooth cross-fade transition to soften theme change
        FadeTransition ftOut = new FadeTransition(Duration.millis(120), root);
        ftOut.setFromValue(1.0);
        ftOut.setToValue(0.72);
        ftOut.setInterpolator(Interpolator.EASE_OUT);
        ftOut.setOnFinished(e -> {
            applyCurrentTheme(root);
            FadeTransition ftIn = new FadeTransition(Duration.millis(200), root);
            ftIn.setFromValue(0.72);
            ftIn.setToValue(1.0);
            ftIn.setInterpolator(Interpolator.EASE_IN);
            ftIn.play();
        });
        ftOut.play();
    }

    private static void applyCurrentTheme(Parent root) {
        if (root == null) return;

        try {
            var resource = ThemeManager.class.getResource("/style.css");
            if (resource != null) {
                String stylesheet = resource.toExternalForm();
                if (!root.getStylesheets().contains(stylesheet)) {
                    root.getStylesheets().add(stylesheet);
                }
            } else {
                System.err.println("ThemeManager: style.css not found in resources!");
            }
        } catch (Exception e) {
            System.err.println("ThemeManager: Error loading stylesheet: " + e.getMessage());
        }

        // Toggle dark style class
        if (darkMode) {
            if (!root.getStyleClass().contains("dark")) {
                root.getStyleClass().add("dark");
            }
        } else {
            root.getStyleClass().remove("dark");
        }
    }
}
