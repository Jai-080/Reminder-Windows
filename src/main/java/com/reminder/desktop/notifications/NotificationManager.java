package com.reminder.desktop.notifications;

import java.awt.*;
import java.awt.image.BufferedImage;

public class NotificationManager {
    private static NotificationManager instance;
    private TrayIcon trayIcon;
    private boolean traySupported;

    private NotificationManager() {
        initTray();
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }

    private void initTray() {
        if (!SystemTray.isSupported()) {
            System.err.println("SystemTray is not supported on this platform.");
            traySupported = false;
            return;
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();
            
            // Create a dynamically generated 16x16 icon to avoid external resource issues
            BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(127, 119, 221)); // Accent purple #7F77DD
            g2d.fillRoundRect(2, 2, 12, 12, 4, 4);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            g2d.drawString("R", 4, 12);
            g2d.dispose();

            trayIcon = new TrayIcon(image, "Reminder Desktop");
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            traySupported = true;
        } catch (Exception e) {
            System.err.println("Failed to initialize system tray icon: " + e.getMessage());
            traySupported = false;
        }
    }

    public void showNotification(String title, String message) {
        if (traySupported && trayIcon != null) {
            // Show balloon message
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        } else {
            // Fallback console log if tray icon is not initialized/supported
            System.out.println("NOTIFICATION: [" + title + "] " + message);
        }
    }

    public void showWarningNotification(String title, String message) {
        if (traySupported && trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.WARNING);
        } else {
            System.out.println("WARNING NOTIFICATION: [" + title + "] " + message);
        }
    }
}
