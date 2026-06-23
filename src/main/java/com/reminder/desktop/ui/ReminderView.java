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
        formTitle.getStyleClass().add("section-header");
        formTitle.setStyle("-fx-font-size: 14px;");

        HBox inputsRow = new HBox(12);
        inputsRow.setAlignment(Pos.CENTER_LEFT);

        TextField textInput = new TextField();
        textInput.setPromptText("Reminder message...");
        HBox.setHgrow(textInput, Priority.ALWAYS);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(150);
        UIUtils.configureDatePicker(datePicker);

        java.time.LocalTime nowTime = java.time.LocalTime.now();
        TimePicker timePicker = new TimePicker(nowTime.getHour(), nowTime.getMinute());

        Button scheduleBtn = new Button("Schedule");
        scheduleBtn.getStyleClass().add("button-accent");

        inputsRow.getChildren().addAll(textInput, datePicker, new Label("Time:"), timePicker, scheduleBtn);
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
        pendingTitle.getStyleClass().add("section-header");
        pendingTitle.setStyle("-fx-font-size: 16px;");
        pendingContainer = new VBox(10);
        pendingBox.getChildren().addAll(pendingTitle, pendingContainer);
        grid.add(pendingBox, 0, 0);

        // Expired Section
        VBox expiredBox = new VBox(12);
        Label expiredTitle = new Label("Fired & Expired");
        expiredTitle.getStyleClass().add("section-header");
        expiredTitle.setStyle("-fx-font-size: 16px;");
        expiredContainer = new VBox(10);
        expiredBox.getChildren().addAll(expiredTitle, expiredContainer);
        grid.add(expiredBox, 1, 0);

        content.getChildren().addAll(headerBox, formCard, grid);
        this.setContent(content);

        // Form Event
        scheduleBtn.setOnAction(e -> {
            controller.addReminder(textInput.getText(), datePicker.getValue(), timePicker.getHour(), timePicker.getMinute(), this);
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
            VBox emptyBox = new VBox(6);
            emptyBox.getStyleClass().add("empty-state-box");
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(16));
            
            Label iconLbl = new Label("");
            iconLbl.getStyleClass().add("empty-state-icon");
            
            Label titleLbl = new Label("No active reminders");
            titleLbl.getStyleClass().add("empty-state-title");
            
            Label descLbl = new Label("Schedule one using the form above.");
            descLbl.getStyleClass().add("empty-state-desc");
            
            emptyBox.getChildren().addAll(iconLbl, titleLbl, descLbl);
            pendingContainer.getChildren().add(emptyBox);
        } else {
            for (Reminder reminder : pending) {
                pendingContainer.getChildren().add(createReminderCard(reminder, true));
            }
        }

        // Expired List
        if (expired.isEmpty()) {
            VBox emptyBox = new VBox(6);
            emptyBox.getStyleClass().add("empty-state-box");
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(16));
            
            Label iconLbl = new Label("");
            iconLbl.getStyleClass().add("empty-state-icon");
            
            Label titleLbl = new Label("No expired history");
            titleLbl.getStyleClass().add("empty-state-title");
            
            Label descLbl = new Label("Past reminders will show up here.");
            descLbl.getStyleClass().add("empty-state-desc");
            
            emptyBox.getChildren().addAll(iconLbl, titleLbl, descLbl);
            expiredContainer.getChildren().add(emptyBox);
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Sync Badge
        if ("PENDING".equalsIgnoreCase(reminder.getSyncStatus())) {
            Label syncLbl = new Label("Pending");
            syncLbl.getStyleClass().addAll("badge-pill", "badge-pending");
            footer.getChildren().addAll(syncLbl, spacer);
        } else {
            footer.getChildren().addAll(spacer);
        }

        if (isPending) {
            Button rescheduleBtn = new Button("Reschedule");
            rescheduleBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 6;");
            rescheduleBtn.setOnAction(e -> triggerEditDialog(reminder));

            Button delBtn = new Button("Delete");
            delBtn.getStyleClass().add("button-danger");
            delBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 6;");
            delBtn.setOnAction(e -> controller.deleteReminder(reminder, this));

            footer.getChildren().addAll(rescheduleBtn, delBtn);
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
        dialog.setHeaderText("Modify Reminder");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        ThemeManager.registerRoot(dialog.getDialogPane());

        ButtonType saveBtnType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        VBox grid = new VBox(10);
        grid.setPadding(new Insets(10));
        
        TextField textInput = new TextField(reminder.getText());
        
        Instant instant = Instant.ofEpochMilli(reminder.getTime());
        LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        DatePicker datePicker = new DatePicker(ldt.toLocalDate());
        UIUtils.configureDatePicker(datePicker);

        java.time.LocalTime nowTime = java.time.LocalTime.now();
        TimePicker timePicker = new TimePicker(nowTime.getHour(), nowTime.getMinute());

        HBox timeRow = new HBox(6, new Label("Time:"), timePicker);
        timeRow.setAlignment(Pos.CENTER_LEFT);

        grid.getChildren().addAll(new Label("Message:"), textInput, new Label("Date:"), datePicker, timeRow);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveBtnType) {
            controller.updateReminder(reminder, textInput.getText(), datePicker.getValue(), timePicker.getHour(), timePicker.getMinute(), this);
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
