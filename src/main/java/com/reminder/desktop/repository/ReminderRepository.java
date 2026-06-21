package com.reminder.desktop.repository;

import com.reminder.desktop.database.ReminderDao;
import com.reminder.desktop.models.Reminder;
import com.reminder.desktop.notifications.ReminderScheduler;
import com.reminder.desktop.sync.ApiClient;
import com.reminder.desktop.sync.SyncService;

import java.util.ArrayList;
import java.util.List;

public class ReminderRepository {
    private final ReminderDao reminderDao;

    public ReminderRepository() {
        this.reminderDao = new ReminderDao();
    }

    public List<Reminder> getAllReminders() {
        try {
            return reminderDao.getAllReminders();
        } catch (Exception e) {
            System.err.println("Error reading reminders: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Reminder> getPendingReminders() {
        try {
            return reminderDao.getPendingReminders();
        } catch (Exception e) {
            System.err.println("Error reading pending reminders: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Reminder> getExpiredReminders() {
        try {
            return reminderDao.getExpiredReminders();
        } catch (Exception e) {
            System.err.println("Error reading expired reminders: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public Reminder addReminder(String text, long time) throws Exception {
        Reminder reminder = new Reminder(
                null,
                null,
                text,
                time,
                false,
                0L,
                System.currentTimeMillis(),
                "PENDING"
        );

        reminderDao.insertReminder(reminder);

        // Schedule notification
        ReminderScheduler.getInstance().scheduleReminder(reminder);

        // Trigger background sync
        SyncService.getInstance().triggerSyncAsync(null);

        return reminder;
    }

    public void updateReminder(Reminder reminder) throws Exception {
        reminder.setSyncStatus("PENDING");
        reminder.setUpdatedAt(System.currentTimeMillis());
        reminderDao.updateReminder(reminder);

        // Reschedule
        ReminderScheduler.getInstance().rescheduleReminder(reminder);

        // Trigger background sync
        SyncService.getInstance().triggerSyncAsync(null);
    }

    public void snoozeReminder(Reminder reminder, int minutes) throws Exception {
        long snoozedTime = System.currentTimeMillis() + (long) minutes * 60 * 1000;
        reminder.setTime(snoozedTime);
        reminder.setSnoozedTime(snoozedTime);
        reminder.setExpired(false);
        reminder.setSyncStatus("PENDING");
        reminder.setUpdatedAt(System.currentTimeMillis());

        reminderDao.updateReminder(reminder);

        // Reschedule
        ReminderScheduler.getInstance().rescheduleReminder(reminder);

        // Trigger background sync
        SyncService.getInstance().triggerSyncAsync(null);
    }

    public void markExpired(Reminder reminder) throws Exception {
        reminder.setExpired(true);
        reminder.setSyncStatus("PENDING");
        reminder.setUpdatedAt(System.currentTimeMillis());

        reminderDao.updateReminder(reminder);

        // Trigger background sync
        SyncService.getInstance().triggerSyncAsync(null);
    }

    public void deleteReminder(Reminder reminder) throws Exception {
        if (reminder.getId() != null) {
            if (reminder.getServerId() == null) {
                reminderDao.deleteReminder(reminder.getId());
            } else {
                reminderDao.softDeleteReminder(reminder.getId());
            }
        }

        // Cancel schedule
        ReminderScheduler.getInstance().cancelReminder(reminder);

        // Trigger background sync
        SyncService.getInstance().triggerSyncAsync(null);
    }
}
