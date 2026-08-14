package com.reminder.desktop.notifications;

import java.awt.TrayIcon;

public class NotificationManager {
    private static NotificationManager instance;

    private NotificationManager() {
        // Ensure TrayManager is initialized
        TrayManager.getInstance();
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    public void showNotification(String title, String message) {
        TrayManager.getInstance().showNotification(title, message, TrayIcon.MessageType.INFO);
    }

    public void showWarningNotification(String title, String message) {
        TrayManager.getInstance().showNotification(title, message, TrayIcon.MessageType.WARNING);
    }
}

