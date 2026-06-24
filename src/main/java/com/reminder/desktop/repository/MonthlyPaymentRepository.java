package com.reminder.desktop.repository;

import com.reminder.desktop.database.MonthlyPaymentDao;
import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.models.RecurrenceType;
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
        return addPayment(name, rawDueDate, null, RecurrenceType.MONTHLY, "7,3,1,0");
    }

    public MonthlyPayment addPayment(String name, long rawDueDate, Double amount, RecurrenceType recurrence, String notificationOffsets) throws Exception {
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
                "PENDING",
                amount,
                recurrence,
                notificationOffsets
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
        if (payment.getRecurrence() == RecurrenceType.ONE_TIME) {
            payment.setLastPaidAt(System.currentTimeMillis());
            payment.setCompleted(false);
            payment.setSyncStatus("PENDING");
            payment.setUpdatedAt(System.currentTimeMillis());
            paymentDao.updatePayment(payment);

            // Cancel alarm, do not reschedule
            ReminderScheduler.getInstance().cancelPayment(payment);
        } else {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(payment.getDueDate());
            if (payment.getRecurrence() == RecurrenceType.QUARTERLY) {
                cal.add(Calendar.MONTH, 3);
            } else if (payment.getRecurrence() == RecurrenceType.YEARLY) {
                cal.add(Calendar.YEAR, 1);
            } else {
                cal.add(Calendar.MONTH, 1);
            }
            long newDueDate = cal.getTimeInMillis();

            payment.setDueDate(newDueDate);
            payment.setLastPaidAt(System.currentTimeMillis());
            payment.setCompleted(false);
            payment.setSyncStatus("PENDING");
            payment.setUpdatedAt(System.currentTimeMillis());
            paymentDao.updatePayment(payment);

            // Reschedule (cancel previous, schedule next)
            ReminderScheduler.getInstance().cancelPayment(payment);
            ReminderScheduler.getInstance().schedulePayment(payment);
        }

        // Trigger background sync
        SyncService.getInstance().triggerSyncAsync(null);
    }

    public void deletePayment(MonthlyPayment payment) throws Exception {
        if (payment.getId() != null) {
            if (payment.getServerId() == null) {
                paymentDao.deletePayment(payment.getId());
            } else {
                paymentDao.softDeletePayment(payment.getId());
            }
        }

        // Cancel schedule
        ReminderScheduler.getInstance().cancelPayment(payment);

        // Trigger background sync
        SyncService.getInstance().triggerSyncAsync(null);
    }
}
