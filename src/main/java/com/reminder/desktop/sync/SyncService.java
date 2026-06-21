package com.reminder.desktop.sync;

import com.reminder.desktop.auth.TokenStorage;
import com.reminder.desktop.database.QuickNoteDao;
import com.reminder.desktop.database.ReminderDao;
import com.reminder.desktop.database.MonthlyPaymentDao;
import com.reminder.desktop.dto.QuickNoteDto;
import com.reminder.desktop.dto.ReminderDto;
import com.reminder.desktop.dto.MonthlyPaymentDto;
import com.reminder.desktop.models.QuickNote;
import com.reminder.desktop.models.Reminder;
import com.reminder.desktop.models.MonthlyPayment;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SyncService {
    private static SyncService instance;
    private final ApiClient apiClient;
    private final QuickNoteDao noteDao;
    private final ReminderDao reminderDao;
    private final MonthlyPaymentDao paymentDao;
    private final ExecutorService executor;

    // Phase 10: Concurrency safety and exponential backoff
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);
    private long retryDelayMs = 5000;
    private static final long MAX_RETRY_DELAY_MS = 300000;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "SyncScheduler");
        t.setDaemon(true);
        return t;
    });
    private ScheduledFuture<?> retryFuture;
    private ScheduledFuture<?> periodicSyncFuture;

    public interface SyncListener {
        void onSyncStarted();
        void onSyncFinished(boolean success, String message);
    }

    private SyncService() {
        this.apiClient = ApiClient.getInstance();
        this.noteDao = new QuickNoteDao();
        this.reminderDao = new ReminderDao();
        this.paymentDao = new MonthlyPaymentDao();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SyncThread");
            t.setDaemon(true);
            return t;
        });
    }

    public static synchronized SyncService getInstance() {
        if (instance == null) {
            instance = new SyncService();
        }
        return instance;
    }

    private synchronized void resetRetryDelay() {
        retryDelayMs = 5000;
        if (retryFuture != null) {
            retryFuture.cancel(false);
            retryFuture = null;
        }
    }

    private synchronized void scheduleRetry() {
        if (retryFuture != null && !retryFuture.isDone()) {
            return;
        }
        System.out.println("Scheduling sync retry in " + (retryDelayMs / 1000) + " seconds...");
        retryFuture = scheduler.schedule(() -> {
            triggerSyncAsync(null);
        }, retryDelayMs, TimeUnit.MILLISECONDS);
        retryDelayMs = Math.min(retryDelayMs * 2, MAX_RETRY_DELAY_MS);
    }

    public void syncAll() {
        triggerSyncAsync(null);
    }

    public synchronized void startPeriodicSync() {
        if (periodicSyncFuture != null && !periodicSyncFuture.isDone()) {
            return;
        }
        System.out.println("WebSocketSync: Starting 5-minute periodic sync background scheduler.");
        periodicSyncFuture = scheduler.scheduleAtFixedRate(() -> {
            if (TokenStorage.hasToken()) {
                System.out.println("WebSocketSync: Periodic background sync triggered.");
                syncAll();
            }
        }, 5, 5, TimeUnit.MINUTES);
    }

    public synchronized void stopPeriodicSync() {
        if (periodicSyncFuture != null) {
            System.out.println("WebSocketSync: Stopping periodic sync background scheduler.");
            periodicSyncFuture.cancel(true);
            periodicSyncFuture = null;
        }
    }

    public void triggerSyncAsync(SyncListener listener) {
        if (!TokenStorage.hasToken()) {
            if (listener != null) listener.onSyncFinished(false, "Not authenticated.");
            return;
        }
        
        executor.submit(() -> {
            if (!syncRunning.compareAndSet(false, true)) {
                System.out.println("Sync already in progress, skipping trigger.");
                if (listener != null) listener.onSyncFinished(false, "Sync already in progress.");
                return;
            }
            if (listener != null) listener.onSyncStarted();
            try {
                performSync();
                TokenStorage.setLastSyncTimestamp(System.currentTimeMillis());
                resetRetryDelay(); // Success: reset backoff
                if (listener != null) listener.onSyncFinished(true, "Synchronized successfully.");
            } catch (Exception e) {
                System.err.println("Synchronization failed: " + e.getMessage());
                scheduleRetry(); // Failure: schedule backoff retry
                if (listener != null) listener.onSyncFinished(false, e.getMessage());
            } finally {
                syncRunning.set(false);
            }
        });
    }

    public void performSync() throws Exception {
        syncNotes();
        syncReminders();
        syncPayments();
    }

    private void syncNotes() throws Exception {
        // 0. Process Deletions First
        List<QuickNote> deletedNotes = noteDao.getDeletedNotes();
        java.util.Set<Long> deletedServerIds = new java.util.HashSet<>();
        for (QuickNote note : deletedNotes) {
            if (note.getServerId() != null) {
                deletedServerIds.add(note.getServerId());
                try {
                    apiClient.delete("/api/notes/" + note.getServerId());
                    note.setSyncStatus("DELETE_SYNCED");
                    noteDao.deleteNote(note.getId());
                } catch (Exception e) {
                    if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().toLowerCase().contains("not found"))) {
                        noteDao.deleteNote(note.getId());
                    } else {
                        System.err.println("Failed to sync delete note offline: " + e.getMessage());
                        throw e;
                    }
                }
            } else {
                noteDao.deleteNote(note.getId());
            }
        }

        // 1. Pull from Server (Direct array reading to optimize performance)
        QuickNoteDto[] serverNotes = apiClient.get("/api/notes", QuickNoteDto[].class);
        
        java.util.Set<Long> serverIds = new java.util.HashSet<>();
        if (serverNotes != null) {
            for (QuickNoteDto dto : serverNotes) {
                if (dto.getId() != null) {
                    serverIds.add(dto.getId());
                }
            }
        }

        // Deletion propagation: prune local records that have been deleted on the server.
        // We only prune records that are currently "SYNCED" locally.
        // "PENDING" records represent local modifications that should NOT be deleted.
        List<QuickNote> localNotes = noteDao.getAllNotes();
        for (QuickNote local : localNotes) {
            if (local.getServerId() != null && "SYNCED".equals(local.getSyncStatus())) {
                if (!serverIds.contains(local.getServerId())) {
                    noteDao.deleteNote(local.getId());
                }
            }
        }

        if (serverNotes != null) {
            for (QuickNoteDto dto : serverNotes) {
                if (dto.getId() != null && deletedServerIds.contains(dto.getId())) {
                    continue; // Skip locally deleted notes
                }

                QuickNote localNote = noteDao.getNoteByServerId(dto.getId());
                long serverMillis = parseInstant(dto.getUpdatedAt());

                if (localNote != null) {
                    if (serverMillis > localNote.getUpdatedAt()) {
                        // Overwrite local with server changes
                        localNote.setText(dto.getText());
                        localNote.setCompleted(dto.getIsCompleted());
                        localNote.setPosition(dto.getPosition() != null ? dto.getPosition() : 0);
                        localNote.setUpdatedAt(serverMillis);
                        localNote.setSyncStatus("SYNCED");
                        noteDao.updateNote(localNote);
                    }
                } else {
                    // Insert new server note locally
                    QuickNote newNote = new QuickNote(
                            null,
                            dto.getId(),
                            dto.getText(),
                            dto.getIsCompleted() != null && dto.getIsCompleted(),
                            dto.getPosition() != null ? dto.getPosition() : 0,
                            serverMillis,
                            "SYNCED"
                    );
                    noteDao.insertNote(newNote);
                }
            }
        }

        // 2. Push Pending to Server
        List<QuickNote> pendingNotes = noteDao.getPendingNotes();
        for (QuickNote local : pendingNotes) {
            QuickNoteDto dto = new QuickNoteDto(local.getServerId(), local.getText(), local.isCompleted(), local.getPosition());
            dto.setUpdatedAt(String.valueOf(local.getUpdatedAt()));
            if (local.getServerId() == null) {
                // POST to create
                QuickNoteDto responseDto = apiClient.post("/api/notes", dto, QuickNoteDto.class);
                local.setServerId(responseDto.getId());
                local.setSyncStatus("SYNCED");
                local.setUpdatedAt(parseInstant(responseDto.getUpdatedAt()));
                noteDao.updateNote(local);
            } else {
                // PUT to update
                QuickNoteDto responseDto = apiClient.put("/api/notes/" + local.getServerId(), dto, QuickNoteDto.class);
                local.setSyncStatus("SYNCED");
                local.setUpdatedAt(parseInstant(responseDto.getUpdatedAt()));
                noteDao.updateNote(local);
            }
        }
    }

    private void syncReminders() throws Exception {
        // 0. Process Deletions First
        List<Reminder> deletedReminders = reminderDao.getDeletedReminders();
        java.util.Set<Long> deletedServerIds = new java.util.HashSet<>();
        for (Reminder reminder : deletedReminders) {
            if (reminder.getServerId() != null) {
                deletedServerIds.add(reminder.getServerId());
                try {
                    apiClient.delete("/api/reminders/" + reminder.getServerId());
                    reminder.setSyncStatus("DELETE_SYNCED");
                    reminderDao.deleteReminder(reminder.getId());
                } catch (Exception e) {
                    if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().toLowerCase().contains("not found"))) {
                        reminderDao.deleteReminder(reminder.getId());
                    } else {
                        System.err.println("Failed to sync delete reminder offline: " + e.getMessage());
                        throw e;
                    }
                }
            } else {
                reminderDao.deleteReminder(reminder.getId());
            }
        }

        // 1. Pull from Server (Direct array reading to optimize performance)
        ReminderDto[] serverReminders = apiClient.get("/api/reminders", ReminderDto[].class);

        java.util.Set<Long> serverIds = new java.util.HashSet<>();
        if (serverReminders != null) {
            for (ReminderDto dto : serverReminders) {
                if (dto.getId() != null) {
                    serverIds.add(dto.getId());
                }
            }
        }

        // Deletion propagation: prune local records that have been deleted on the server.
        // We only prune records that are currently "SYNCED" locally.
        // "PENDING" records represent local modifications that should NOT be deleted.
        List<Reminder> localReminders = reminderDao.getAllReminders();
        for (Reminder local : localReminders) {
            if (local.getServerId() != null && "SYNCED".equals(local.getSyncStatus())) {
                if (!serverIds.contains(local.getServerId())) {
                    reminderDao.deleteReminder(local.getId());
                    com.reminder.desktop.notifications.ReminderScheduler.getInstance().cancelReminder(local);
                }
            }
        }

        if (serverReminders != null) {
            for (ReminderDto dto : serverReminders) {
                if (dto.getId() != null && deletedServerIds.contains(dto.getId())) {
                    continue; // Skip locally deleted reminders
                }

                Reminder localReminder = reminderDao.getReminderByServerId(dto.getId());
                long serverMillis = parseInstant(dto.getUpdatedAt());

                if (localReminder != null) {
                    if (serverMillis > localReminder.getUpdatedAt()) {
                        localReminder.setText(dto.getText());
                        localReminder.setTime(dto.getReminderTime());
                        localReminder.setExpired(dto.getIsExpired() != null && dto.getIsExpired());
                        localReminder.setSnoozedTime(dto.getSnoozedTime() != null ? dto.getSnoozedTime() : 0L);
                        localReminder.setUpdatedAt(serverMillis);
                        localReminder.setSyncStatus("SYNCED");
                        reminderDao.updateReminder(localReminder);
                        com.reminder.desktop.notifications.ReminderScheduler.getInstance().cancelReminder(localReminder);
                        if (!localReminder.isExpired() && localReminder.getTime() > System.currentTimeMillis()) {
                            com.reminder.desktop.notifications.ReminderScheduler.getInstance().scheduleReminder(localReminder);
                        }
                    }
                } else {
                    Reminder newReminder = new Reminder(
                            null,
                            dto.getId(),
                            dto.getText(),
                            dto.getReminderTime() != null ? dto.getReminderTime() : 0L,
                            dto.getIsExpired() != null && dto.getIsExpired(),
                            dto.getSnoozedTime() != null ? dto.getSnoozedTime() : 0L,
                            serverMillis,
                            "SYNCED"
                    );
                    reminderDao.insertReminder(newReminder);
                    if (!newReminder.isExpired() && newReminder.getTime() > System.currentTimeMillis()) {
                        com.reminder.desktop.notifications.ReminderScheduler.getInstance().scheduleReminder(newReminder);
                    }
                }
            }
        }

        // 2. Push Pending to Server
        List<Reminder> pendingReminders = reminderDao.getSyncPendingReminders();
        for (Reminder local : pendingReminders) {
            ReminderDto dto = new ReminderDto(
                    local.getServerId(),
                    local.getText(),
                    local.getTime(),
                    local.isExpired(),
                    local.getSnoozedTime()
            );
            dto.setUpdatedAt(String.valueOf(local.getUpdatedAt()));
            if (local.getServerId() == null) {
                ReminderDto responseDto = apiClient.post("/api/reminders", dto, ReminderDto.class);
                local.setServerId(responseDto.getId());
                local.setSyncStatus("SYNCED");
                local.setUpdatedAt(parseInstant(responseDto.getUpdatedAt()));
                reminderDao.updateReminder(local);
            } else {
                ReminderDto responseDto = apiClient.put("/api/reminders/" + local.getServerId(), dto, ReminderDto.class);
                local.setSyncStatus("SYNCED");
                local.setUpdatedAt(parseInstant(responseDto.getUpdatedAt()));
                reminderDao.updateReminder(local);
            }
        }
    }

    private void syncPayments() throws Exception {
        // 0. Process Deletions First
        List<MonthlyPayment> deletedPayments = paymentDao.getDeletedPayments();
        java.util.Set<Long> deletedServerIds = new java.util.HashSet<>();
        for (MonthlyPayment payment : deletedPayments) {
            if (payment.getServerId() != null) {
                deletedServerIds.add(payment.getServerId());
                try {
                    apiClient.delete("/api/payments/" + payment.getServerId());
                    payment.setSyncStatus("DELETE_SYNCED");
                    paymentDao.deletePayment(payment.getId());
                } catch (Exception e) {
                    if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().toLowerCase().contains("not found"))) {
                        paymentDao.deletePayment(payment.getId());
                    } else {
                        System.err.println("Failed to sync delete payment offline: " + e.getMessage());
                        throw e;
                    }
                }
            } else {
                paymentDao.deletePayment(payment.getId());
            }
        }

        // 1. Pull from Server (Direct array reading to optimize performance)
        MonthlyPaymentDto[] serverPayments = apiClient.get("/api/payments", MonthlyPaymentDto[].class);

        java.util.Set<Long> serverIds = new java.util.HashSet<>();
        if (serverPayments != null) {
            for (MonthlyPaymentDto dto : serverPayments) {
                if (dto.getId() != null) {
                    serverIds.add(dto.getId());
                }
            }
        }

        // Deletion propagation: prune local records that have been deleted on the server.
        // We only prune records that are currently "SYNCED" locally.
        // "PENDING" records represent local modifications that should NOT be deleted.
        List<MonthlyPayment> localPayments = paymentDao.getAllPayments();
        for (MonthlyPayment local : localPayments) {
            if (local.getServerId() != null && "SYNCED".equals(local.getSyncStatus())) {
                if (!serverIds.contains(local.getServerId())) {
                    paymentDao.deletePayment(local.getId());
                    com.reminder.desktop.notifications.ReminderScheduler.getInstance().cancelPayment(local);
                }
            }
        }

        if (serverPayments != null) {
            long startOfToday = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            for (MonthlyPaymentDto dto : serverPayments) {
                if (dto.getId() != null && deletedServerIds.contains(dto.getId())) {
                    continue; // Skip locally deleted payments
                }

                MonthlyPayment localPayment = paymentDao.getPaymentByServerId(dto.getId());
                long serverMillis = parseInstant(dto.getUpdatedAt());

                if (localPayment != null) {
                    if (serverMillis > localPayment.getUpdatedAt()) {
                        localPayment.setName(dto.getName());
                        localPayment.setDueDate(dto.getDueDate());
                        localPayment.setCompleted(dto.getCompleted() != null && dto.getCompleted());
                        localPayment.setUpdatedAt(serverMillis);
                        localPayment.setSyncStatus("SYNCED");
                        paymentDao.updatePayment(localPayment);
                        com.reminder.desktop.notifications.ReminderScheduler.getInstance().cancelPayment(localPayment);
                        if (!localPayment.isCompleted() && localPayment.getDueDate() >= startOfToday) {
                            com.reminder.desktop.notifications.ReminderScheduler.getInstance().schedulePayment(localPayment);
                        }
                    }
                } else {
                    MonthlyPayment newPayment = new MonthlyPayment(
                            null,
                            dto.getId(),
                            dto.getName(),
                            dto.getDueDate() != null ? dto.getDueDate() : 0L,
                            dto.getCompleted() != null && dto.getCompleted(),
                            serverMillis,
                            "SYNCED"
                    );
                    paymentDao.insertPayment(newPayment);
                    if (!newPayment.isCompleted() && newPayment.getDueDate() >= startOfToday) {
                        com.reminder.desktop.notifications.ReminderScheduler.getInstance().schedulePayment(newPayment);
                    }
                }
            }
        }

        // 2. Push Pending to Server
        List<MonthlyPayment> pendingPayments = paymentDao.getSyncPendingPayments();
        for (MonthlyPayment local : pendingPayments) {
            MonthlyPaymentDto dto = new MonthlyPaymentDto(
                    local.getServerId(),
                    local.getName(),
                    local.getDueDate(),
                    local.isCompleted()
            );
            dto.setUpdatedAt(String.valueOf(local.getUpdatedAt()));
            if (local.getServerId() == null) {
                MonthlyPaymentDto responseDto = apiClient.post("/api/payments", dto, MonthlyPaymentDto.class);
                local.setServerId(responseDto.getId());
                local.setSyncStatus("SYNCED");
                local.setUpdatedAt(parseInstant(responseDto.getUpdatedAt()));
                paymentDao.updatePayment(local);
            } else {
                MonthlyPaymentDto responseDto = apiClient.put("/api/payments/" + local.getServerId(), dto, MonthlyPaymentDto.class);
                local.setSyncStatus("SYNCED");
                local.setUpdatedAt(parseInstant(responseDto.getUpdatedAt()));
                paymentDao.updatePayment(local);
            }
        }
    }

    private long parseInstant(String instantStr) {
        if (instantStr == null || instantStr.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            return Instant.parse(instantStr).toEpochMilli();
        } catch (Exception e) {
            try {
                // Fallback: try parsing double or long representation
                return Long.parseLong(instantStr);
            } catch (Exception ex) {
                return System.currentTimeMillis();
            }
        }
    }
}
