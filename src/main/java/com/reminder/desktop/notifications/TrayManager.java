package com.reminder.desktop.notifications;

import com.reminder.desktop.MainApplication;
import javafx.application.Platform;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;

public class TrayManager {
    private static TrayManager instance;
    private TrayIcon trayIcon;
    private boolean traySupported;

    private TrayManager() {
        initTray();
    }

    public static synchronized TrayManager getInstance() {
        if (instance == null) {
            instance = new TrayManager();
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

            // Load icon from resources
            Image image = null;
            try {
                URL imageURL = TrayManager.class.getResource("/ic_launcher.png");
                if (imageURL != null) {
                    image = ImageIO.read(imageURL);
                }
            } catch (Exception e) {
                System.err.println("Could not load /ic_launcher.png for system tray: " + e.getMessage());
            }

            // Fallback: Dynamically generate 16x16 icon
            if (image == null) {
                System.out.println("Using fallback dynamically generated system tray icon.");
                BufferedImage fallbackImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = fallbackImage.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(127, 119, 221)); // Accent purple #7F77DD
                g2d.fillRoundRect(2, 2, 12, 12, 4, 4);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.drawString("R", 4, 12);
                g2d.dispose();
                image = fallbackImage;
            }

            // Create context menu
            PopupMenu popup = new PopupMenu();
            
            MenuItem openItem = new MenuItem("Open Reminder");
            openItem.addActionListener(e -> Platform.runLater(() -> {
                if (MainApplication.getInstance() != null) {
                    MainApplication.getInstance().showAndFocus();
                }
            }));
            popup.add(openItem);

            popup.addSeparator();

            MenuItem exitItem = new MenuItem("Exit Reminder");
            exitItem.addActionListener(e -> {
                if (MainApplication.getInstance() != null) {
                    MainApplication.getInstance().exitApplication();
                }
            });
            popup.add(exitItem);

            // Create TrayIcon
            trayIcon = new TrayIcon(image, "Reminder Desktop", popup);
            trayIcon.setImageAutoSize(true);

            // Left-click to open/restore window
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) { // Left click
                        Platform.runLater(() -> {
                            if (MainApplication.getInstance() != null) {
                                MainApplication.getInstance().showAndFocus();
                            }
                        });
                    }
                }
            });

            // Action listener as secondary trigger (e.g. double click on some platforms)
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                if (MainApplication.getInstance() != null) {
                    MainApplication.getInstance().showAndFocus();
                }
            }));

            tray.add(trayIcon);
            traySupported = true;
            System.out.println("System tray icon successfully initialized.");
        } catch (Exception e) {
            System.err.println("Failed to initialize system tray icon: " + e.getMessage());
            traySupported = false;
        }
    }

    public void showNotification(String title, String message, TrayIcon.MessageType type) {
        if (traySupported && trayIcon != null) {
            trayIcon.displayMessage(title, message, type);
        } else {
            System.out.println("NOTIFICATION [" + type + "]: [" + title + "] " + message);
        }
    }

    public void shutdown() {
        if (traySupported && trayIcon != null) {
            try {
                SystemTray.getSystemTray().remove(trayIcon);
                System.out.println("System tray icon successfully removed.");
            } catch (Exception e) {
                System.err.println("Failed to remove system tray icon: " + e.getMessage());
            }
            trayIcon = null;
            traySupported = false;
        }
    }
}
