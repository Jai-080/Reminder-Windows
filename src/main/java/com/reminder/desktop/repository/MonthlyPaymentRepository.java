package com.reminder.desktop.repository;

import com.reminder.desktop.database.MonthlyPaymentDao;
import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.notifications.ReminderScheduler;
import com.reminder.desktop.sync.ApiClient;
import com.reminder.desktop.sync.SyncService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MonthlyPaymentRepository {
    private final MonthlyPaymentDao paymentDao;

    public MonthlyPaymentRepository() {
        this.paymentDao = new MonthlyPaymentDao();
    }

    public List<MonthlyPayment> getAllPayments() {
        try {
            return paymentDao.getAllPayments();
        } catch (Exception e) {
            System.err.println("Error reading payments: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public MonthlyPayment addPayment(String name, long rawDueDate) throws Exception {
        // Normalize due date to 9:00 AM on that day
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(rawDueDate);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long normalizedDueDate = cal.getTimeInMillis();

        MonthlyPayment payment = new MonthlyPayment(
                null,
                null,
                name,
                normalizedDueDate,
                false,
                System.currentTimeMillis(),
                "PENDING"
        );

        paymentDao.insertPayment(payment);

        // Schedule payment alert
        ReminderScheduler.getInstance().schedulePayment(payment);

        // Trigger background sync
        SyncService.getInstance().triggerSyncAsync(null);

        return payment;
    }

    public void updatePayment(MonthlyPayment payment) throws Exception {
        payment.setSyncStatus("PENDING");
        payment.setUpdatedAt(System.currentTimeMillis());
        paymentDao.updatePayment(payment);

        // Reschedule payment alert
        ReminderScheduler.getInstance().reschedulePayment(payment);

        // Trigger background sync
        SyncService.getInstance().triggerSyncAsync(null);
    }

    public void togglePaymentCompleted(MonthlyPayment payment) throws Exception {
        payment.setCompleted(!payment.isCompleted());
        payment.setSyncStatus("PENDING");
        payment.setUpdatedAt(System.currentTimeMillis());
        paymentDao.updatePayment(payment);

        // If marked completed, we can cancel the active schedule, otherwise schedule it
        if (payment.isCompleted()) {
            ReminderScheduler.getInstance().cancelPayment(payment);
        } else {
            ReminderScheduler.getInstance().schedulePayment(payment);
        }

        // Trigger background sync
        SyncService.getInstance().triggerSyncAsync(null);
    }

    public void deletePayment(MonthlyPayment payment) throws Exception {
        if (payment.getId() != null) {
            paymentDao.deletePayment(payment.getId());
        }

        // Cancel schedule
        ReminderScheduler.getInstance().cancelPayment(payment);

        if (payment.getServerId() != null) {
            long serverId = payment.getServerId();
            new Thread(() -> {
                try {
                    ApiClient.getInstance().delete("/api/payments/" + serverId);
                } catch (Exception e) {
                    System.err.println("Failed to delete payment on server: " + e.getMessage());
                }
            }).start();
        }
    }
}
