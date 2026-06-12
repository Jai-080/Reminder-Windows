package com.reminder.desktop.notifications;

import com.reminder.desktop.database.ReminderDao;
import com.reminder.desktop.database.MonthlyPaymentDao;
import com.reminder.desktop.models.Reminder;
import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.sync.SyncService;
import javafx.application.Platform;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ReminderScheduler {
    private static ReminderScheduler instance;
    private final ScheduledThreadPoolExecutor executor;
    private final Map<String, ScheduledFuture<?>> scheduledTasks;
    private final List<ReminderScheduleListener> listeners;
    private final ReminderDao reminderDao;
    private final MonthlyPaymentDao paymentDao;

    public interface ReminderScheduleListener {
        void onReminderExpired(Reminder reminder);
        void onPaymentDue(MonthlyPayment payment);
    }

    private ReminderScheduler() {
        this.executor = new ScheduledThreadPoolExecutor(4);
        this.executor.setRemoveOnCancelPolicy(true);
        this.scheduledTasks = new ConcurrentHashMap<>();
        this.listeners = new CopyOnWriteArrayList<>();
        this.reminderDao = new ReminderDao();
        this.paymentDao = new MonthlyPaymentDao();
    }

    public static synchronized ReminderScheduler getInstance() {
        if (instance == null) {
            instance = new ReminderScheduler();
        }
        return instance;
    }

    public void registerListener(ReminderScheduleListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(ReminderScheduleListener listener) {
        listeners.remove(listener);
    }

    public void initialize() {
        System.out.println("Initializing ReminderScheduler and loading pending reminders/payments...");
        cancelAll();
        
        executor.submit(() -> {
            try {
                // Load Reminders
                List<Reminder> pendingReminders = reminderDao.getPendingReminders();
                long now = System.currentTimeMillis();
                for (Reminder r : pendingReminders) {
                    if (r.getTime() <= now) {
                        // Trigger immediately since it was missed while the app was closed
                        triggerReminder(r);
                    } else {
                        scheduleReminder(r);
                    }
                }

                // Load Payments
                List<MonthlyPayment> allPayments = paymentDao.getAllPayments();
                long startOfToday = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                for (MonthlyPayment p : allPayments) {
                    if (!p.isCompleted()) {
                        // Avoid triggering popup notifications for historical uncompleted monthly payments
                        if (p.getDueDate() >= startOfToday) {
                            if (p.getDueDate() <= now) {
                                triggerPayment(p);
                            } else {
                                schedulePayment(p);
                            }
                        } else {
                            System.out.println("Skipping alert for historical uncompleted payment: " + p.getName());
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("Failed to load reminders from SQLite on scheduler startup: " + e.getMessage());
            }
        });
    }

    public void scheduleReminder(Reminder reminder) {
        if (reminder.getId() == null) return;
        
        String key = "reminder_" + reminder.getId();
        cancelTask(key);

        long delay = reminder.getTime() - System.currentTimeMillis();
        if (delay < 0) delay = 0;

        ScheduledFuture<?> future = executor.schedule(() -> triggerReminder(reminder), delay, TimeUnit.MILLISECONDS);
        scheduledTasks.put(key, future);
        System.out.printf("[REMINDER SCHEDULER] Scheduling reminder: localId=%d, serverId=%d, time=%d, success=true%n",
                reminder.getId(), reminder.getServerId() != null ? reminder.getServerId() : -1, reminder.getTime());
    }

    public void cancelReminder(Reminder reminder) {
        if (reminder.getId() == null) return;
        cancelTask("reminder_" + reminder.getId());
    }

    public void rescheduleReminder(Reminder reminder) {
        scheduleReminder(reminder);
    }

    public void schedulePayment(MonthlyPayment payment) {
        if (payment.getId() == null) return;

        String key = "payment_" + payment.getId();
        cancelTask(key);

        long delay = payment.getDueDate() - System.currentTimeMillis();
        if (delay < 0) delay = 0;

        ScheduledFuture<?> future = executor.schedule(() -> triggerPayment(payment), delay, TimeUnit.MILLISECONDS);
        scheduledTasks.put(key, future);
        System.out.printf("[PAYMENT SCHEDULER] Scheduling payment: localId=%d, serverId=%d, dueDate=%d, success=true%n",
                payment.getId(), payment.getServerId() != null ? payment.getServerId() : -1, payment.getDueDate());
    }

    public void cancelPayment(MonthlyPayment payment) {
        if (payment.getId() == null) return;
        cancelTask("payment_" + payment.getId());
    }

    public void reschedulePayment(MonthlyPayment payment) {
        schedulePayment(payment);
    }

    private void cancelTask(String key) {
        ScheduledFuture<?> future = scheduledTasks.remove(key);
        if (future != null) {
            future.cancel(true);
        }
    }

    public void cancelAll() {
        for (String key : scheduledTasks.keySet()) {
            cancelTask(key);
        }
    }

    private void triggerReminder(Reminder reminder) {
        System.out.println("Triggering reminder: " + reminder.getText());
        
        // Show Notification
        NotificationManager.getInstance().showNotification("Timed Reminder", reminder.getText());

        try {
            // Update Database State
            reminder.setExpired(true);
            reminder.setSyncStatus("PENDING");
            reminder.setUpdatedAt(System.currentTimeMillis());
            reminderDao.updateReminder(reminder);

            // Sync with backend
            SyncService.getInstance().triggerSyncAsync(null);

            // Notify listeners on JavaFX Thread
            Platform.runLater(() -> {
                for (ReminderScheduleListener l : listeners) {
                    l.onReminderExpired(reminder);
                }
            });
        } catch (Exception e) {
            System.err.println("Error updating reminder on expiration: " + e.getMessage());
        }
    }

    private void triggerPayment(MonthlyPayment payment) {
        System.out.println("Triggering monthly payment alert: " + payment.getName());

        // Show Notification
        NotificationManager.getInstance().showWarningNotification("Payment Due Today", "Payment is due: " + payment.getName());

        // Notify listeners on JavaFX Thread
        Platform.runLater(() -> {
            for (ReminderScheduleListener l : listeners) {
                l.onPaymentDue(payment);
            }
        });
    }
}
