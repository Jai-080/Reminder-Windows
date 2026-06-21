package com.reminder.desktop.ui;

import com.reminder.desktop.models.Reminder;
import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.notifications.ReminderScheduler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ReminderView extends ScrollPane implements ReminderScheduler.ReminderScheduleListener {
    private final ReminderController controller;
    private final VBox pendingContainer;
    private final VBox expiredContainer;
    private final SimpleDateFormat dateFormat;

    public ReminderView() {
        this.controller = new ReminderController();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
        
        this.setFitToWidth(true);
        this.setPannable(true);

        VBox content = new VBox(24);
        content.setPadding(new Insets(32));
        content.setAlignment(Pos.TOP_LEFT);

        // Header Title
        Label title = new Label("Timed Reminders");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label subtitle = new Label("Configure exact notifications. Supports snoozing actions.");
        subtitle.getStyleClass().add("subtitle-label");
        VBox headerBox = new VBox(4, title, subtitle);

        // Add Reminder Form
        VBox formCard = new VBox(12);
        formCard.getStyleClass().add("card");
        formCard.setPadding(new Insets(16));

        Label formTitle = new Label("Schedule New Reminder");
        formTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox inputsRow = new HBox(12);
        inputsRow.setAlignment(Pos.CENTER_LEFT);

        TextField textInput = new TextField();
        textInput.setPromptText("Reminder message...");
        HBox.setHgrow(textInput, Priority.ALWAYS);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(140);

        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 9);
        hourSpinner.setPrefWidth(70);
        Spinner<Integer> minSpinner = new Spinner<>(0, 59, 0);
        minSpinner.setPrefWidth(70);

        Button scheduleBtn = new Button("Schedule");
        scheduleBtn.getStyleClass().add("button-accent");

        inputsRow.getChildren().addAll(textInput, datePicker, new Label("Time:"), hourSpinner, new Label(":"), minSpinner, scheduleBtn);
        formCard.getChildren().addAll(formTitle, inputsRow);

        // Lists Grid Layout
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col1, col2);

        // Pending Section
        VBox pendingBox = new VBox(12);
        Label pendingTitle = new Label("Active Reminders");
        pendingTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        pendingContainer = new VBox(10);
        pendingBox.getChildren().addAll(pendingTitle, pendingContainer);
        grid.add(pendingBox, 0, 0);

        // Expired Section
        VBox expiredBox = new VBox(12);
        Label expiredTitle = new Label("Fired & Expired");
        expiredTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        expiredContainer = new VBox(10);
        expiredBox.getChildren().addAll(expiredTitle, expiredContainer);
        grid.add(expiredBox, 1, 0);

        content.getChildren().addAll(headerBox, formCard, grid);
        this.setContent(content);

        // Form Event
        scheduleBtn.setOnAction(e -> {
            controller.addReminder(textInput.getText(), datePicker.getValue(), hourSpinner.getValue(), minSpinner.getValue(), this);
            textInput.clear();
        });

        // Register schedule listeners to refresh dynamically on fire
        ReminderScheduler.getInstance().registerListener(this);

        // Initial Load
        controller.loadReminders(this);

        // Dynamic sync finished listener registration
        Runnable syncListener = () -> {
            System.out.println("ReminderView: Sync completed, reloading reminders.");
            controller.loadReminders(this);
        };

        // Auto cleanup listener on layout unload
        this.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                com.reminder.desktop.sync.SyncService.addSyncFinishedListener(syncListener);
                controller.loadReminders(this);
            } else {
                com.reminder.desktop.sync.SyncService.removeSyncFinishedListener(syncListener);
                ReminderScheduler.getInstance().unregisterListener(this);
            }
        });

        if (this.getScene() != null) {
            com.reminder.desktop.sync.SyncService.addSyncFinishedListener(syncListener);
        }
    }

    public void displayReminders(List<Reminder> pending, List<Reminder> expired) {
        pendingContainer.getChildren().clear();
        expiredContainer.getChildren().clear();

        // Pending List
        if (pending.isEmpty()) {
            pendingContainer.getChildren().add(new Label("No active reminders scheduled."));
        } else {
            for (Reminder reminder : pending) {
                pendingContainer.getChildren().add(createReminderCard(reminder, true));
            }
        }

        // Expired List
        if (expired.isEmpty()) {
            expiredContainer.getChildren().add(new Label("No expired reminders in history."));
        } else {
            for (Reminder reminder : expired) {
                expiredContainer.getChildren().add(createReminderCard(reminder, false));
            }
        }
    }

    private VBox createReminderCard(Reminder reminder, boolean isPending) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12));

        Label textLbl = new Label(reminder.getText());
        textLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        textLbl.setWrapText(true);

        Label timeLbl = new Label("Scheduled: " + dateFormat.format(new Date(reminder.getTime())));
        timeLbl.getStyleClass().add("subtitle-label");

        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);

        // Sync Badge
        Label syncLbl = new Label();
        syncLbl.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 4px;");
        if ("PENDING".equalsIgnoreCase(reminder.getSyncStatus())) {
            syncLbl.setText("Pending");
            syncLbl.setStyle(syncLbl.getStyle() + " -fx-background-color: -color-accent-light; -fx-text-fill: -color-accent;");
        } else {
            syncLbl.setText("Synced");
            syncLbl.setStyle(syncLbl.getStyle() + " -fx-background-color: #E2F6EA; -fx-text-fill: -color-success;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footer.getChildren().addAll(syncLbl, spacer);

        if (isPending) {
            // Expire manually button
            Button expireBtn = new Button("Expire");
            expireBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 6;");
            expireBtn.setOnAction(e -> controller.markExpired(reminder, this));

            // Snooze Split/Menu button
            MenuButton snoozeBtn = new MenuButton("Snooze");
            snoozeBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 4;");
            MenuItem sn1 = new MenuItem("1 Min");
            sn1.setOnAction(e -> controller.snoozeReminder(reminder, 1, this));
            MenuItem sn5 = new MenuItem("5 Min");
            sn5.setOnAction(e -> controller.snoozeReminder(reminder, 5, this));
            MenuItem sn10 = new MenuItem("10 Min");
            sn10.setOnAction(e -> controller.snoozeReminder(reminder, 10, this));
            snoozeBtn.getItems().addAll(sn1, sn5, sn10);

            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 6;");
            editBtn.setOnAction(e -> triggerEditDialog(reminder));

            Button delBtn = new Button("Delete");
            delBtn.getStyleClass().add("button-danger");
            delBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 6;");
            delBtn.setOnAction(e -> controller.deleteReminder(reminder, this));

            footer.getChildren().addAll(expireBtn, snoozeBtn, editBtn, delBtn);
        } else {
            // Expired card controls
            Button rescheduleBtn = new Button("Reschedule");
            rescheduleBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 6;");
            rescheduleBtn.setOnAction(e -> triggerEditDialog(reminder));

            Button delBtn = new Button("Delete");
            delBtn.getStyleClass().add("button-danger");
            delBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 6;");
            delBtn.setOnAction(e -> controller.deleteReminder(reminder, this));

            footer.getChildren().addAll(rescheduleBtn, delBtn);
        }

        card.getChildren().addAll(textLbl, timeLbl, footer);
        return card;
    }

    private void triggerEditDialog(Reminder reminder) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Reminder");
        dialog.setHeaderText("Modify the text and trigger schedule:");

        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        VBox grid = new VBox(10);
        grid.setPadding(new Insets(10));
        
        TextField textInput = new TextField(reminder.getText());
        
        Instant instant = Instant.ofEpochMilli(reminder.getTime());
        LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        DatePicker datePicker = new DatePicker(ldt.toLocalDate());
        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, ldt.getHour());
        Spinner<Integer> minSpinner = new Spinner<>(0, 59, ldt.getMinute());

        HBox timeRow = new HBox(6, new Label("Time:"), hourSpinner, new Label(":"), minSpinner);
        timeRow.setAlignment(Pos.CENTER_LEFT);

        grid.getChildren().addAll(new Label("Message:"), textInput, new Label("Date:"), datePicker, timeRow);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtnType) {
            controller.updateReminder(reminder, textInput.getText(), datePicker.getValue(), hourSpinner.getValue(), minSpinner.getValue(), this);
        }
    }

    // Listener Callbacks from Scheduler when fired in real-time
    @Override
    public void onReminderExpired(Reminder reminder) {
        Platform.runLater(() -> controller.loadReminders(this));
    }

    @Override
    public void onPaymentDue(MonthlyPayment payment) {
        // Ignored in reminder view
    }
}
