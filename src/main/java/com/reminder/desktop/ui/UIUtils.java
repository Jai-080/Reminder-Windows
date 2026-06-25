package com.reminder.desktop.ui;

import javafx.scene.control.DatePicker;
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
        message = message.replace("115.99.50.73:50000", "the server")
                         .replace("115.99.50.73", "the server")
                         .replace("localhost:8080", "the server")
                         .replace("10.0.2.2:8080", "the server");
        return message;
    }
}
