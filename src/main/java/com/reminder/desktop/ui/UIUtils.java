package com.reminder.desktop.ui;

import com.reminder.desktop.config.ServerConfig;
import javafx.scene.control.DatePicker;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class UIUtils {
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void configureDatePicker(DatePicker datePicker) {
        if (datePicker == null) return;
        
        datePicker.setEditable(true);
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return DISPLAY_FORMATTER.format(date);
                }
                return "";
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.trim().isEmpty()) {
                    String trimmed = string.trim();
                    try {
                        return LocalDate.parse(trimmed, DISPLAY_FORMATTER);
                    } catch (Exception e) {
                        try {
                            return LocalDate.parse(trimmed, ISO_FORMATTER);
                        } catch (Exception ex) {
                            return LocalDate.now();
                        }
                    }
                }
                return null;
            }
        });
    }

    /** Fills the empty-state icon slot the source left blank: a checklist glyph for Notes. */
    public static SVGPath checklistIcon() {
        return emptyStateIcon("M9 11 L12 14 L22 4 M21 12 L21 19 A2 2 0 0 1 19 21 L5 21 A2 2 0 0 1 3 19 L3 5 A2 2 0 0 1 5 3 L16 3");
    }

    /** Fills the empty-state icon slot for Reminders (both the Active and Expired lists). */
    public static SVGPath bellIcon() {
        return emptyStateIcon("M6 8 A6 6 0 0 1 18 8 C18 15 21 17 21 17 L3 17 C3 17 6 15 6 8 Z M10 20 A2 2 0 0 0 14 20");
    }

    /** Fills the empty-state icon slot for Payments. */
    public static SVGPath walletIcon() {
        return emptyStateIcon("M4 7 A2 2 0 0 1 6 5 L18 5 A2 2 0 0 1 20 7 L20 17 A2 2 0 0 1 18 19 L6 19 A2 2 0 0 1 4 17 Z M14 11 A2 2 0 0 0 14 15 L20 15 L20 11 Z");
    }

    private static SVGPath emptyStateIcon(String pathData) {
        SVGPath icon = new SVGPath();
        icon.setContent(pathData);
        icon.getStyleClass().add("empty-state-icon-shape");
        return icon;
    }

    public static String sanitizeError(String message) {
        if (message == null) {
            return null;
        }
        String serverUrl = com.reminder.desktop.auth.TokenStorage.getServerUrl();
        if (serverUrl != null && !serverUrl.isEmpty()) {
            message = message.replace(serverUrl, "the server");
            String hostOnly = serverUrl.replace("http://", "").replace("https://", "");
            if (!hostOnly.isEmpty()) {
                message = message.replace(hostOnly, "the server");
            }
        }
        // Fallbacks
        String fallbackHost = ServerConfig.getServerHost();
        if (!fallbackHost.isEmpty()) {
            message = message.replace(fallbackHost, "the server");
            if (fallbackHost.contains(":")) {
                message = message.replace(fallbackHost.split(":")[0], "the server");
            }
        }
        message = message.replace("localhost:8080", "the server")
                         .replace("10.0.2.2:8080", "the server");
        return message;
    }
}
