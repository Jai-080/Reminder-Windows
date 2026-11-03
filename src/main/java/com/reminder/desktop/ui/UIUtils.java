package com.reminder.desktop.ui;

import com.reminder.desktop.config.ServerConfig;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
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
        datePicker.setShowWeekNumbers(false);
        datePicker.setCursor(javafx.scene.Cursor.HAND);

        Platform.runLater(() -> {
            if (datePicker.getEditor() != null) {
                datePicker.getEditor().setCursor(javafx.scene.Cursor.HAND);
                datePicker.getEditor().setOnMouseClicked(e -> {
                    if (!datePicker.isShowing()) {
                        datePicker.show();
                    }
                });
            }
            var arrowBtn = datePicker.lookup(".arrow-button");
            if (arrowBtn != null) {
                arrowBtn.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-border-width: 0;");
            }
        });

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

    public static void styleDialog(Dialog<?> dialog) {
        if (dialog == null) return;
        DialogPane pane = dialog.getDialogPane();
        try {
            var res = UIUtils.class.getResource("/style.css");
            if (res != null) {
                String css = res.toExternalForm();
                if (!pane.getStylesheets().contains(css)) {
                    pane.getStylesheets().add(css);
                }
            }
            ThemeManager.registerRoot(pane);
        } catch (Exception ignored) {}

        // Style standard buttons inside button-bar with theme classes
        Platform.runLater(() -> {
            for (ButtonType bt : pane.getButtonTypes()) {
                var node = pane.lookupButton(bt);
                if (node instanceof Button btn) {
                    if (bt.getButtonData() == ButtonBar.ButtonData.OK_DONE
                            || bt == ButtonType.OK
                            || bt == ButtonType.APPLY
                            || bt == ButtonType.YES) {
                        if (!btn.getStyleClass().contains("button-accent")) {
                            btn.getStyleClass().add("button-accent");
                        }
                    } else {
                        if (!btn.getStyleClass().contains("button")) {
                            btn.getStyleClass().add("button");
                        }
                    }
                }
            }
        });

        dialog.setOnHidden(e -> {
            try {
                ThemeManager.unregisterRoot(pane);
            } catch (Exception ignored) {}
        });
    }

    public static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        styleDialog(alert);
        alert.showAndWait();
    }

    public static void showErrorAlert(String title, String content) {
        showAlert(Alert.AlertType.WARNING, title, content);
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
        if (message == null || message.trim().isEmpty()) {
            return "An unexpected error occurred. Please try again.";
        }

        String lower = message.toLowerCase();

        // 1. Network connectivity and resolution errors
        if (lower.contains("connectexception") || lower.contains("failed to connect")
                || lower.contains("unable to resolve host") || lower.contains("unknownhostexception")
                || lower.contains("no address associated") || lower.contains("network is unreachable")
                || lower.contains("connection refused")) {
            return "Unable to connect to the server. Please check your internet connection.";
        }

        // 2. Timeout errors
        if (lower.contains("sockettimeoutexception") || lower.contains("timed out") || lower.contains("timeout")) {
            return "Connection timed out. Please try again.";
        }

        // 3. SSL / Security handshake failures
        if (lower.contains("sslhandshakeexception") || lower.contains("certpathvalidatorexception")
                || lower.contains("sslpeerunverifiedexception")) {
            return "Secure connection to server failed. Please verify your network security settings.";
        }

        // 4. Authentication / Authorization errors
        if (lower.contains("401") || lower.contains("unauthorized")) {
            return "Session expired or invalid credentials. Please sign in again.";
        }
        if (lower.contains("403") || lower.contains("forbidden")) {
            return "Access denied. You do not have permission to perform this action.";
        }

        // 5. Server-side failures (5xx)
        if (lower.contains("500") || lower.contains("502") || lower.contains("503") || lower.contains("504")
                || lower.contains("internal server error") || lower.contains("bad gateway")) {
            return "Server is temporarily unavailable. Please try again later.";
        }

        // 6. JSON / Deserialization parsing failures
        if (lower.contains("jsonsyntaxexception") || lower.contains("malformedjsonexception")
                || lower.contains("jsonparseexception")) {
            return "Received an invalid response format from the server.";
        }

        // 7. Generic raw unhandled Java exceptions
        if (message.contains("java.lang.") || message.contains("java.net.") || message.contains("Exception:")) {
            return "An unexpected error occurred. Please try again.";
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
