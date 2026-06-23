package com.reminder.desktop.ui;

import com.reminder.desktop.models.MonthlyPayment;
import java.time.LocalDate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class PaymentView extends ScrollPane {
    private final PaymentController controller;
    private final VBox listContainer;
    private final SimpleDateFormat dateFormat;

    public PaymentView() {
        this.controller = new PaymentController();
        this.dateFormat = new SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH);

        this.setFitToWidth(true);
        this.setPannable(true);

        VBox content = new VBox(24);
        content.setPadding(new Insets(32));
        content.setAlignment(Pos.TOP_LEFT);

        // Header Title
        Label title = new Label("Monthly Payments");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        Label subtitle = new Label("Track your monthly expenses. Triggers foreground alerts on due dates.");
        subtitle.getStyleClass().add("subtitle-label");
        VBox headerBox = new VBox(4, title, subtitle);

        // Form Card
        VBox formCard = new VBox(12);
        formCard.getStyleClass().add("card");
        formCard.setPadding(new Insets(16));

        Label formTitle = new Label("Add Monthly Payment");
        formTitle.getStyleClass().add("section-header");
        formTitle.setStyle("-fx-font-size: 14px;");

        HBox inputsRow = new HBox(12);
        inputsRow.setAlignment(Pos.CENTER_LEFT);

        TextField nameInput = new TextField();
        nameInput.setPromptText("Eg: Rent...");
        HBox.setHgrow(nameInput, Priority.ALWAYS);

        TextField amountInput = new TextField();
        amountInput.setPromptText("Amount");
        amountInput.setPrefWidth(120);
        amountInput.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty() || newText.matches("\\d*\\.?\\d*")) {
                return change;
            }
            return null;
        }));

        ComboBox<String> recurrenceCombo = new ComboBox<>();
        recurrenceCombo.getItems().addAll("Monthly", "Quarterly", "Yearly");
        recurrenceCombo.setValue("Monthly");
        recurrenceCombo.setPrefWidth(120);

        DatePicker dueDatePicker = new DatePicker(LocalDate.now());
        dueDatePicker.setPrefWidth(150);
        UIUtils.configureDatePicker(dueDatePicker);

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("button-accent");

        inputsRow.getChildren().addAll(nameInput, amountInput, recurrenceCombo, new Label("Due:"), dueDatePicker, addBtn);
        formCard.getChildren().addAll(formTitle, inputsRow);

        // List Header
        Label listTitle = new Label("Payments List");
        listTitle.getStyleClass().add("section-header");
        listTitle.setStyle("-fx-font-size: 16px;");

        listContainer = new VBox(12);
        listContainer.setAlignment(Pos.TOP_LEFT);

        content.getChildren().addAll(headerBox, formCard, listTitle, listContainer);
        this.setContent(content);

        // Event Trigger
        addBtn.setOnAction(e -> {
            controller.addPayment(nameInput.getText(), dueDatePicker.getValue(), amountInput.getText(), recurrenceCombo.getValue(), this);
            nameInput.clear();
            amountInput.clear();
            recurrenceCombo.setValue("Monthly");
        });

        // Initial Load
        controller.loadPayments(this);

        // Dynamic sync finished listener registration
        Runnable syncListener = () -> {
            System.out.println("PaymentView: Sync completed, reloading payments.");
            controller.loadPayments(this);
        };

        this.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                com.reminder.desktop.sync.SyncService.addSyncFinishedListener(syncListener);
                controller.loadPayments(this);
            } else {
                com.reminder.desktop.sync.SyncService.removeSyncFinishedListener(syncListener);
            }
        });

        if (this.getScene() != null) {
            com.reminder.desktop.sync.SyncService.addSyncFinishedListener(syncListener);
        }
    }

    public void displayPayments(List<MonthlyPayment> payments) {
        listContainer.getChildren().clear();

        if (payments.isEmpty()) {
            VBox emptyBox = new VBox(8);
            emptyBox.getStyleClass().add("empty-state-box");
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(24));
            
            Label iconLbl = new Label("💳");
            iconLbl.getStyleClass().add("empty-state-icon");
            
            Label titleLbl = new Label("No monthly payments yet");
            titleLbl.getStyleClass().add("empty-state-title");
            
            Label descLbl = new Label("Set up expenses and due dates above.");
            descLbl.getStyleClass().add("empty-state-desc");
            
            emptyBox.getChildren().addAll(iconLbl, titleLbl, descLbl);
            listContainer.getChildren().add(emptyBox);
            return;
        }

        for (MonthlyPayment payment : payments) {
            HBox paymentCard = new HBox(16);
            paymentCard.getStyleClass().add("card");
            paymentCard.setAlignment(Pos.CENTER_LEFT);
            paymentCard.setPadding(new Insets(12, 16, 12, 16));

            // Complete Checkbox
            CheckBox completeBox = new CheckBox();
            completeBox.setSelected(payment.isCompleted());
            completeBox.setOnAction(e -> controller.toggleComplete(payment, this));

            // Text VBox details
            VBox details = new VBox(4);
            Label nameLbl = new Label(payment.getName());
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            String amountText = payment.getAmount() == null ? "Amount Unknown" : String.format("₹%.2f", payment.getAmount());
            Label amountLbl = new Label(amountText);
            amountLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -color-text-secondary;");

            String recurrenceText = payment.getRecurrence() != null ? payment.getRecurrence().name() : "MONTHLY";
            Label recurrenceBadge = new Label(recurrenceText);
            recurrenceBadge.getStyleClass().addAll("badge-pill", "badge-neutral");

            HBox amountAndRecurrence = new HBox(8, amountLbl, recurrenceBadge);
            amountAndRecurrence.setAlignment(Pos.CENTER_LEFT);

            Label dateLbl = new Label("Due Date: " + dateFormat.format(new Date(payment.getDueDate())));
            dateLbl.getStyleClass().add("subtitle-label");
            
            if (payment.isCompleted()) {
                nameLbl.setStyle(nameLbl.getStyle() + " -fx-strikethrough: true; -fx-text-fill: -color-text-secondary;");
                amountLbl.setStyle(amountLbl.getStyle() + " -fx-strikethrough: true;");
            }
            details.getChildren().addAll(nameLbl, amountAndRecurrence, dateLbl);
            HBox.setHgrow(details, Priority.ALWAYS);

            // Controls VBox
            Button delBtn = new Button("Delete");
            delBtn.getStyleClass().add("button-danger");
            delBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
            delBtn.setOnAction(e -> controller.deletePayment(payment, this));

            HBox controls;
            if ("PENDING".equalsIgnoreCase(payment.getSyncStatus())) {
                Label syncLbl = new Label("Pending");
                syncLbl.getStyleClass().addAll("badge-pill", "badge-pending");
                controls = new HBox(12, syncLbl, delBtn);
            } else {
                controls = new HBox(12, delBtn);
            }
            controls.setAlignment(Pos.CENTER_RIGHT);

            paymentCard.getChildren().addAll(completeBox, details, controls);
            listContainer.getChildren().add(paymentCard);
        }
    }
}
