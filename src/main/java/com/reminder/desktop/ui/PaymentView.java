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
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");

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
        formTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox inputsRow = new HBox(12);
        inputsRow.setAlignment(Pos.CENTER_LEFT);

        TextField nameInput = new TextField();
        nameInput.setPromptText("E.g., Netflix subscription, Rent...");
        HBox.setHgrow(nameInput, Priority.ALWAYS);

        DatePicker dueDatePicker = new DatePicker(LocalDate.now());
        dueDatePicker.setPrefWidth(150);

        Button addBtn = new Button("Add Payment");
        addBtn.getStyleClass().add("button-accent");

        inputsRow.getChildren().addAll(nameInput, new Label("Due Date:"), dueDatePicker, addBtn);
        formCard.getChildren().addAll(formTitle, inputsRow);

        // List Header
        Label listTitle = new Label("Payments List");
        listTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        listContainer = new VBox(12);
        listContainer.setAlignment(Pos.TOP_LEFT);

        content.getChildren().addAll(headerBox, formCard, listTitle, listContainer);
        this.setContent(content);

        // Event Trigger
        addBtn.setOnAction(e -> {
            controller.addPayment(nameInput.getText(), dueDatePicker.getValue(), this);
            nameInput.clear();
        });

        // Initial Load
        controller.loadPayments(this);
    }

    public void displayPayments(List<MonthlyPayment> payments) {
        listContainer.getChildren().clear();

        if (payments.isEmpty()) {
            Label placeholder = new Label("No monthly payments added. Set one up above!");
            placeholder.getStyleClass().add("subtitle-label");
            listContainer.getChildren().add(placeholder);
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
            
            Label dateLbl = new Label("Due Date: " + dateFormat.format(new Date(payment.getDueDate())));
            dateLbl.getStyleClass().add("subtitle-label");
            
            if (payment.isCompleted()) {
                nameLbl.setStyle(nameLbl.getStyle() + " -fx-strikethrough: true; -fx-text-fill: -color-text-secondary;");
            }
            details.getChildren().addAll(nameLbl, dateLbl);
            HBox.setHgrow(details, Priority.ALWAYS);

            // Sync Badge
            Label syncLbl = new Label();
            syncLbl.setStyle("-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 4px;");
            if ("PENDING".equalsIgnoreCase(payment.getSyncStatus())) {
                syncLbl.setText("Pending");
                syncLbl.setStyle(syncLbl.getStyle() + " -fx-background-color: -color-accent-light; -fx-text-fill: -color-accent;");
            } else {
                syncLbl.setText("Synced");
                syncLbl.setStyle(syncLbl.getStyle() + " -fx-background-color: #E2F6EA; -fx-text-fill: -color-success;");
            }

            // Controls VBox
            Button delBtn = new Button("Delete");
            delBtn.getStyleClass().add("button-danger");
            delBtn.setStyle("-fx-padding: 4 8; -fx-font-size: 11px;");
            delBtn.setOnAction(e -> controller.deletePayment(payment, this));

            HBox controls = new HBox(12, syncLbl, delBtn);
            controls.setAlignment(Pos.CENTER_RIGHT);

            paymentCard.getChildren().addAll(completeBox, details, controls);
            listContainer.getChildren().add(paymentCard);
        }
    }
}
