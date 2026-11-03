package com.reminder.desktop.repository;

import com.reminder.desktop.database.MonthlyPaymentDao;
import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.models.RecurrenceType;
import com.reminder.desktop.notifications.ReminderScheduler;
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
        return addPayment(name, rawDueDate, null, RecurrenceType.MONTHLY, 28, "7,3,1,0");
    }

    public MonthlyPayment addPayment(String name, long rawDueDate, Double amount, RecurrenceType recurrence, String notificationOffsets) throws Exception {
        return addPayment(name, rawDueDate, amount, recurrence, 28, notificationOffsets);
    }

    public MonthlyPayment addPayment(String name, long rawDueDate, Double amount, RecurrenceType recurrence, int customIntervalDays, String notificationOffsets) throws Exception {
        if (recurrence == RecurrenceType.CUSTOM_DAYS) {
            if (customIntervalDays < 1 || customIntervalDays > 365) {
                throw new IllegalArgumentException("Custom recurrence interval must be between 1 and 365 days.");
            }
        }

        // Normalize due date to 9:00 AM on that day
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(rawDueDate);
        cal.set(Calendar.HOUR_OF_DAY, 9);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long normalizedDueDate = cal.getTimeInMillis();
        int originalDueDay = cal.get(Calendar.DAY_OF_MONTH);

        String syncStatus = (recurrence != null && recurrence.isLocalOnly()) ? "LOCAL_ONLY" : "PENDING";
        int interval = (recurrence == RecurrenceType.CUSTOM_DAYS) ? customIntervalDays : 28;

        MonthlyPayment payment = new MonthlyPayment(
                null,
                null,
                name,
                normalizedDueDate,
                false,
                System.currentTimeMillis(),
                syncStatus,
                amount,
                recurrence,
                notificationOffsets,
                originalDueDay,
                interval
        );

        paymentDao.insertPayment(payment);

        // Schedule payment alert
        ReminderScheduler.getInstance().schedulePayment(payment);

        // Trigger background sync if not LOCAL_ONLY
        if (!"LOCAL_ONLY".equals(syncStatus)) {
            SyncService.getInstance().triggerSyncAsync(null);
        }

        return payment;
    }

    public void updatePayment(MonthlyPayment payment) throws Exception {
        if (payment.getRecurrence() == RecurrenceType.CUSTOM_DAYS) {
            if (payment.getCustomIntervalDays() < 1 || payment.getCustomIntervalDays() > 365) {
                throw new IllegalArgumentException("Custom recurrence interval must be between 1 and 365 days.");
            }
        }
        if (payment.getRecurrence() != null && payment.getRecurrence().isLocalOnly()) {
            payment.setSyncStatus("LOCAL_ONLY");
        } else if (!"SYNCED".equals(payment.getSyncStatus())) {
            payment.setSyncStatus("PENDING");
        }
        payment.setUpdatedAt(System.currentTimeMillis());
        paymentDao.updatePayment(payment);

        // Reschedule payment alert
        ReminderScheduler.getInstance().reschedulePayment(payment);

        // Trigger background sync if not LOCAL_ONLY
        if (!"LOCAL_ONLY".equals(payment.getSyncStatus())) {
            SyncService.getInstance().triggerSyncAsync(null);
        }
    }

    public void togglePaymentCompleted(MonthlyPayment payment) throws Exception {
        if (payment.getRecurrence() == RecurrenceType.ONE_TIME || payment.getRecurrence() == RecurrenceType.UNKNOWN) {
            payment.setLastPaidAt(System.currentTimeMillis());
            payment.setCompleted(true);
            if (payment.getRecurrence() != null && payment.getRecurrence().isLocalOnly()) {
                payment.setSyncStatus("LOCAL_ONLY");
            } else {
                payment.setSyncStatus("PENDING");
            }
            payment.setUpdatedAt(System.currentTimeMillis());
            paymentDao.updatePayment(payment);

            // Cancel alarm, do not reschedule
            ReminderScheduler.getInstance().cancelPayment(payment);
        } else {
            int anchorDay = payment.getOriginalDueDay();
            int customInterval = payment.getCustomIntervalDays();
            long newDueDate = MonthlyPayment.calculateNextDueDate(
                    payment.getDueDate(),
                    payment.getRecurrence(),
                    anchorDay,
                    customInterval,
                    System.currentTimeMillis()
            );

            payment.setDueDate(newDueDate);
            payment.setOriginalDueDay(anchorDay);
            payment.setCustomIntervalDays(customInterval);
            payment.setLastPaidAt(System.currentTimeMillis());
            payment.setCompleted(false);

            if (payment.getRecurrence() != null && payment.getRecurrence().isLocalOnly()) {
                payment.setSyncStatus("LOCAL_ONLY");
            } else {
                payment.setSyncStatus("PENDING");
            }
            payment.setUpdatedAt(System.currentTimeMillis());
            paymentDao.updatePayment(payment);

            // Reschedule (cancel previous, schedule next)
            ReminderScheduler.getInstance().cancelPayment(payment);
            ReminderScheduler.getInstance().schedulePayment(payment);
        }

        if (!"LOCAL_ONLY".equals(payment.getSyncStatus())) {
            SyncService.getInstance().triggerSyncAsync(null);
        }
    }

    public void deletePayment(MonthlyPayment payment) throws Exception {
        if (payment.getId() != null) {
            if (payment.getServerId() == null || "LOCAL_ONLY".equals(payment.getSyncStatus())) {
                paymentDao.deletePayment(payment.getId());
            } else {
                paymentDao.softDeletePayment(payment.getId());
            }
        }

        // Cancel schedule
        ReminderScheduler.getInstance().cancelPayment(payment);

        if (!"LOCAL_ONLY".equals(payment.getSyncStatus())) {
            SyncService.getInstance().triggerSyncAsync(null);
        }
    }
}
