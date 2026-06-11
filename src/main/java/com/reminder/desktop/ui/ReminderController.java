package com.reminder.desktop.ui;

import com.reminder.desktop.models.Reminder;
import com.reminder.desktop.repository.ReminderRepository;
import javafx.application.Platform;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

public class ReminderController {
    private final ReminderRepository repository;

    public ReminderController() {
        this.repository = new ReminderRepository();
    }

    public void loadReminders(ReminderView view) {
        new Thread(() -> {
            try {
                List<Reminder> pending = repository.getPendingReminders();
                List<Reminder> expired = repository.getExpiredReminders();
                
                // Sort lists by time
                pending.sort((r1, r2) -> Long.compare(r1.getTime(), r2.getTime()));
                expired.sort((r1, r2) -> Long.compare(r1.getTime(), r2.getTime()));

                Platform.runLater(() -> view.displayReminders(pending, expired));
            } catch (Exception e) {
                System.err.println("Error loading reminders in background: " + e.getMessage());
            }
        }, "LoadRemindersThread").start();
    }

    public void addReminder(String text, LocalDate date, int hour, int minute, ReminderView view) {
        if (text == null || text.trim().isEmpty() || date == null) return;

        new Thread(() -> {
            try {
                LocalDateTime ldt = LocalDateTime.of(date, LocalTime.of(hour, minute));
                long timeMillis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                repository.addReminder(text.trim(), timeMillis);
                Platform.runLater(() -> loadReminders(view));
            } catch (Exception e) {
                System.err.println("Error adding reminder: " + e.getMessage());
            }
        }).start();
    }

    public void snoozeReminder(Reminder reminder, int minutes, ReminderView view) {
        new Thread(() -> {
            try {
                repository.snoozeReminder(reminder, minutes);
                Platform.runLater(() -> loadReminders(view));
            } catch (Exception e) {
                System.err.println("Error snoozing reminder: " + e.getMessage());
            }
        }).start();
    }

    public void markExpired(Reminder reminder, ReminderView view) {
        new Thread(() -> {
            try {
                repository.markExpired(reminder);
                Platform.runLater(() -> loadReminders(view));
            } catch (Exception e) {
                System.err.println("Error marking reminder expired: " + e.getMessage());
            }
        }).start();
    }

    public void deleteReminder(Reminder reminder, ReminderView view) {
        new Thread(() -> {
            try {
                repository.deleteReminder(reminder);
                Platform.runLater(() -> loadReminders(view));
            } catch (Exception e) {
                System.err.println("Error deleting reminder: " + e.getMessage());
            }
        }).start();
    }

    public void updateReminder(Reminder reminder, String newText, LocalDate date, int hour, int minute, ReminderView view) {
        if (newText == null || newText.trim().isEmpty() || date == null) return;

        new Thread(() -> {
            try {
                LocalDateTime ldt = LocalDateTime.of(date, LocalTime.of(hour, minute));
                long timeMillis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

                reminder.setText(newText.trim());
                reminder.setTime(timeMillis);
                reminder.setExpired(false); // Rescheduled

                repository.updateReminder(reminder);
                Platform.runLater(() -> loadReminders(view));
            } catch (Exception e) {
                System.err.println("Error updating reminder: " + e.getMessage());
            }
        }).start();
    }
}
