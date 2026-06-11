package com.reminder.desktop.ui;

import javafx.scene.Parent;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static boolean darkMode = false;
    private static final List<Parent> registeredRoots = new ArrayList<>();

    public static boolean isDarkMode() {
        return darkMode;
    }

    public static void toggleTheme() {
        setDarkMode(!darkMode);
    }

    public static void setDarkMode(boolean dark) {
        darkMode = dark;
        // Apply theme changes to all open roots
        for (Parent root : new ArrayList<>(registeredRoots)) {
            applyCurrentTheme(root);
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
