package com.reminder.desktop.ui;

import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.repository.MonthlyPaymentRepository;
import javafx.application.Platform;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class PaymentController {
    private final MonthlyPaymentRepository repository;

    public PaymentController() {
        this.repository = new MonthlyPaymentRepository();
    }

    public void loadPayments(PaymentView view) {
        new Thread(() -> {
            try {
                List<MonthlyPayment> payments = repository.getAllPayments();
                payments.sort((p1, p2) -> Long.compare(p1.getDueDate(), p2.getDueDate()));
                Platform.runLater(() -> view.displayPayments(payments));
            } catch (Exception e) {
                System.err.println("Error loading payments in background: " + e.getMessage());
            }
        }, "LoadPaymentsThread").start();
    }

    public void addPayment(String name, LocalDate date, PaymentView view) {
        addPayment(name, date, null, "MONTHLY", view);
    }

    public void addPayment(String name, LocalDate date, String amountStr, String recurrenceStr, PaymentView view) {
        if (name == null || name.trim().isEmpty() || date == null) return;

        new Thread(() -> {
            try {
                long epochMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                Double amount = null;
                if (amountStr != null && !amountStr.trim().isEmpty()) {
                    try {
                        amount = Double.parseDouble(amountStr.trim());
                    } catch (NumberFormatException ignored) {}
                }
                
                com.reminder.desktop.models.RecurrenceType recurrence = com.reminder.desktop.models.RecurrenceType.MONTHLY;
                if (recurrenceStr != null) {
                    try {
                        recurrence = com.reminder.desktop.models.RecurrenceType.valueOf(recurrenceStr.toUpperCase());
                    } catch (IllegalArgumentException ignored) {}
                }

                // Internally default to "7,3,1,0" for offsets
                String notificationOffsets = "7,3,1,0";

                repository.addPayment(name.trim(), epochMillis, amount, recurrence, notificationOffsets);
                Platform.runLater(() -> loadPayments(view));
            } catch (Exception e) {
                System.err.println("Error adding payment: " + e.getMessage());
            }
        }).start();
    }

    public void toggleComplete(MonthlyPayment payment, PaymentView view) {
        new Thread(() -> {
            try {
                repository.togglePaymentCompleted(payment);
                Platform.runLater(() -> loadPayments(view));
            } catch (Exception e) {
                System.err.println("Error toggling payment complete: " + e.getMessage());
            }
        }).start();
    }

    public void deletePayment(MonthlyPayment payment, PaymentView view) {
        new Thread(() -> {
            try {
                repository.deletePayment(payment);
                Platform.runLater(() -> loadPayments(view));
            } catch (Exception e) {
                System.err.println("Error deleting payment: " + e.getMessage());
            }
        }).start();
    }
}
