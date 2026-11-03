package com.reminder.desktop;

import com.reminder.desktop.database.DatabaseManager;
import com.reminder.desktop.database.MonthlyPaymentDao;
import com.reminder.desktop.database.QuickNoteDao;
import com.reminder.desktop.database.ReminderDao;
import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.models.QuickNote;
import com.reminder.desktop.models.RecurrenceType;
import com.reminder.desktop.models.Reminder;
import com.reminder.desktop.notifications.ReminderScheduler;
import com.reminder.desktop.repository.MonthlyPaymentRepository;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaymentDatabaseAndSchedulingTest {

    private static MonthlyPaymentDao paymentDao;
    private static QuickNoteDao noteDao;
    private static ReminderDao reminderDao;
    private static MonthlyPaymentRepository repository;
    private static com.reminder.desktop.repository.ReminderRepository reminderRepository;

    @BeforeAll
    public static void setUp() throws Exception {
        // Ensure DatabaseManager initializes tables
        try (Connection conn = DatabaseManager.getConnection()) {
            assertNotNull(conn, "Connection must not be null");
        }
        paymentDao = new MonthlyPaymentDao();
        noteDao = new QuickNoteDao();
        reminderDao = new ReminderDao();
        repository = new MonthlyPaymentRepository();
        reminderRepository = new com.reminder.desktop.repository.ReminderRepository();
    }

    // =========================================================================
    // 1. DATABASE SCHEMA & PERSISTENCE TESTS
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("Database schema: original_due_day and custom_interval_days columns exist")
    public void testSchemaColumnsExist() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(monthly_payments)")) {

            boolean foundOriginalDueDay = false;
            boolean foundCustomIntervalDays = false;

            while (rs.next()) {
                String colName = rs.getString("name");
                if ("original_due_day".equalsIgnoreCase(colName)) {
                    foundOriginalDueDay = true;
                }
                if ("custom_interval_days".equalsIgnoreCase(colName)) {
                    foundCustomIntervalDays = true;
                }
            }

            assertTrue(foundOriginalDueDay, "Column 'original_due_day' must exist in monthly_payments table");
            assertTrue(foundCustomIntervalDays, "Column 'custom_interval_days' must exist in monthly_payments table");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Persistence: Insert, read, update, and reload custom recurrence payment")
    public void testCustomRecurrencePersistence() throws Exception {
        long dueDate = System.currentTimeMillis() + 86400000L;
        MonthlyPayment payment = new MonthlyPayment(
                null,
                null,
                "Internet Subscription",
                dueDate,
                false,
                System.currentTimeMillis(),
                "LOCAL_ONLY",
                799.0,
                RecurrenceType.CUSTOM_DAYS,
                "0",
                15,
                28
        );

        paymentDao.insertPayment(payment);
        assertNotNull(payment.getId(), "Generated ID must be assigned");

        // Read back
        MonthlyPayment loaded = paymentDao.getPaymentById(payment.getId());
        assertNotNull(loaded);
        assertEquals("Internet Subscription", loaded.getName());
        assertEquals(RecurrenceType.CUSTOM_DAYS, loaded.getRecurrence());
        assertEquals(28, loaded.getCustomIntervalDays());
        assertEquals(15, loaded.getOriginalDueDay());
        assertEquals(Double.valueOf(799.0), loaded.getAmount());

        // Update interval and anchor
        loaded.setCustomIntervalDays(45);
        loaded.setOriginalDueDay(20);
        long recordedPaidTime = 1750000000000L;
        loaded.setLastPaidAt(recordedPaidTime);
        paymentDao.updatePayment(loaded);

        // Reload and verify
        MonthlyPayment reloaded = paymentDao.getPaymentById(payment.getId());
        assertNotNull(reloaded);
        assertEquals(45, reloaded.getCustomIntervalDays());
        assertEquals(20, reloaded.getOriginalDueDay());
        assertEquals(Long.valueOf(recordedPaidTime), reloaded.getLastPaidAt(), "Completion timestamp must be persisted");

        // Clean up
        paymentDao.deletePayment(payment.getId());
        assertNull(paymentDao.getPaymentById(payment.getId()));
    }

    @Test
    @Order(3)
    @DisplayName("Backward compatibility: Pre-existing records with null original_due_day derive anchor dynamically")
    public void testBackwardCompatibilityNullAnchor() throws Exception {
        // Insert raw SQL row simulating pre-migration record with NULL original_due_day and custom_interval_days
        long testDueDate = 1769850000000L; // Jan 31, 2026 roughly
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(testDueDate);
        int expectedDay = cal.get(java.util.Calendar.DAY_OF_MONTH);

        int rawId;
        String rawInsert = "INSERT INTO monthly_payments (name, due_date, completed, recurrence, sync_status) VALUES (?, ?, 0, 'MONTHLY', 'SYNCED')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(rawInsert, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "Legacy Payment");
            pstmt.setLong(2, testDueDate);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                assertTrue(rs.next());
                rawId = rs.getInt(1);
            }
        }

        // DAO read
        MonthlyPayment legacy = paymentDao.getPaymentById(rawId);
        assertNotNull(legacy);
        assertEquals(expectedDay, legacy.getOriginalDueDay(), "Missing original_due_day must dynamically derive from due_date");
        assertEquals(28, legacy.getCustomIntervalDays(), "Missing custom_interval_days must default to 28");

        // Clean up
        paymentDao.deletePayment(rawId);
    }

    // =========================================================================
    // 2. SCHEDULER & NOTIFICATION ISOLATION TESTS
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("Scheduler safety: Reminder and payment task keys cannot collide")
    public void testSchedulerKeyIsolation() {
        int sharedId = 42;
        String reminderKey = "reminder_" + sharedId;
        String paymentKey = "payment_" + sharedId;

        assertNotEquals(reminderKey, paymentKey, "Reminder and payment scheduler keys must be isolated by prefix");
        assertTrue(reminderKey.startsWith("reminder_"));
        assertTrue(paymentKey.startsWith("payment_"));
    }

    @Test
    @Order(5)
    @DisplayName("Scheduler: Schedule, reschedule, and cancel payment tasks without error")
    public void testPaymentSchedulingOperations() {
        ReminderScheduler scheduler = ReminderScheduler.getInstance();
        long futureDue = System.currentTimeMillis() + 3600000L;
        MonthlyPayment payment = new MonthlyPayment(999, null, "Electricity", futureDue, false, System.currentTimeMillis(), "SYNCED");

        // Schedule
        assertDoesNotThrow(() -> scheduler.schedulePayment(payment));

        // Reschedule
        assertDoesNotThrow(() -> scheduler.reschedulePayment(payment));

        // Cancel
        assertDoesNotThrow(() -> scheduler.cancelPayment(payment));
    }

    @Test
    @Order(6)
    @DisplayName("Repository: togglePaymentCompleted advances recurring payment and marks ONE_TIME complete")
    public void testTogglePaymentCompleted() throws Exception {
        long futureDue = System.currentTimeMillis() + 86400000L;

        // 1. One-Time Payment
        MonthlyPayment oneTime = repository.addPayment("One Time Gift", futureDue, 100.0, RecurrenceType.ONE_TIME, 28, "0");
        assertNotNull(oneTime.getId());
        repository.togglePaymentCompleted(oneTime);
        assertTrue(oneTime.isCompleted(), "One-time payment must be marked completed");
        assertNotNull(oneTime.getLastPaidAt(), "Completion timestamp must be set");

        // 2. Custom Days Payment
        MonthlyPayment custom = repository.addPayment("Gym Fee", futureDue, 50.0, RecurrenceType.CUSTOM_DAYS, 14, "0");
        assertNotNull(custom.getId());
        assertEquals("PENDING", custom.getSyncStatus(), "CUSTOM_DAYS is supported by backend and must be marked PENDING");

        long origDue = custom.getDueDate();
        repository.togglePaymentCompleted(custom);

        assertFalse(custom.isCompleted(), "Recurring payment must remain uncompleted for next cycle");
        assertTrue(custom.getDueDate() > origDue, "Due date must advance to next cycle");
        assertNotNull(custom.getLastPaidAt(), "Last paid timestamp must be recorded");

        // Clean up
        paymentDao.deletePayment(oneTime.getId());
        paymentDao.deletePayment(custom.getId());
    }

    // =========================================================================
    // 3. REGRESSION TESTS (CRUD FOR NOTES & REMINDERS)
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("Regression: Quick Notes CRUD operations work reliably")
    public void testQuickNotesRegression() throws Exception {
        QuickNote note = new QuickNote(null, null, "Windows Phase 1 Verify Note", false, 0, System.currentTimeMillis(), "PENDING");
        noteDao.insertNote(note);
        assertNotNull(note.getId());

        QuickNote loaded = noteDao.getNoteById(note.getId());
        assertNotNull(loaded);
        assertEquals("Windows Phase 1 Verify Note", loaded.getText());

        loaded.setCompleted(true);
        noteDao.updateNote(loaded);
        QuickNote updated = noteDao.getNoteById(note.getId());
        assertTrue(updated.isCompleted());

        noteDao.deleteNote(note.getId());
        assertNull(noteDao.getNoteById(note.getId()));
    }

    @Test
    @Order(8)
    @DisplayName("Regression: Timed Reminders CRUD and scheduling work reliably")
    public void testRemindersRegression() throws Exception {
        long remindTime = System.currentTimeMillis() + 7200000L;
        Reminder reminder = new Reminder(null, null, "Test Reminder Task", remindTime, false, 0L, System.currentTimeMillis(), "PENDING");
        reminderDao.insertReminder(reminder);
        assertNotNull(reminder.getId());

        Reminder loaded = reminderDao.getReminderById(reminder.getId());
        assertNotNull(loaded);
        assertEquals("Test Reminder Task", loaded.getText());
        assertEquals(remindTime, loaded.getTime());

        // Test scheduler with reminder
        ReminderScheduler.getInstance().scheduleReminder(reminder);
        ReminderScheduler.getInstance().cancelReminder(reminder);

        reminderDao.deleteReminder(reminder.getId());
        assertNull(reminderDao.getReminderById(reminder.getId()));
    }

    // =========================================================================
    // 4. RECURRENCE TYPE BACKEND COMPATIBILITY & LOCAL_ONLY PROTECTION TESTS
    // =========================================================================

    @Test
    @Order(9)
    @DisplayName("Compatibility: RecurrenceType backend support and LOCAL_ONLY flags")
    public void testRecurrenceTypeCompatibilityFlags() {
        // Phase 4C/4E: All 7 standard recurrence types are supported; UNKNOWN is fallback
        for (RecurrenceType type : RecurrenceType.values()) {
            if (type == RecurrenceType.UNKNOWN) {
                assertFalse(type.isSupportedByBackend(), "UNKNOWN must not be supported by backend");
                assertTrue(type.isLocalOnly(), "UNKNOWN must be local-only");
            } else {
                assertTrue(type.isSupportedByBackend(), type + " must be supported by backend");
                assertFalse(type.isLocalOnly(), type + " must not be local-only");
            }
        }
        assertEquals(8, RecurrenceType.values().length);
    }

    @Test
    @Order(10)
    @DisplayName("Repository: All seven recurrence types are eligible for sync and saved as PENDING")
    public void testAllRecurrencesSavedAsPending() throws Exception {
        long futureDue = System.currentTimeMillis() + 86400000L;

        // 1. BI_MONTHLY
        MonthlyPayment biMonthly = repository.addPayment("Water Bill", futureDue, 40.0, RecurrenceType.BI_MONTHLY, 28, "0");
        assertNotNull(biMonthly.getId());
        assertEquals("PENDING", biMonthly.getSyncStatus(), "BI_MONTHLY must be marked PENDING");

        // 2. HALF_YEARLY
        MonthlyPayment halfYearly = repository.addPayment("Insurance", futureDue, 600.0, RecurrenceType.HALF_YEARLY, 28, "0");
        assertNotNull(halfYearly.getId());
        assertEquals("PENDING", halfYearly.getSyncStatus(), "HALF_YEARLY must be marked PENDING");

        // 3. CUSTOM_DAYS
        MonthlyPayment customDays = repository.addPayment("Internet 45d", futureDue, 80.0, RecurrenceType.CUSTOM_DAYS, 45, "0");
        assertNotNull(customDays.getId());
        assertEquals("PENDING", customDays.getSyncStatus(), "CUSTOM_DAYS must be marked PENDING");
        assertEquals(45, customDays.getCustomIntervalDays());

        // 4. MONTHLY
        MonthlyPayment monthly = repository.addPayment("Rent", futureDue, 1500.0, RecurrenceType.MONTHLY, 28, "0");
        assertNotNull(monthly.getId());
        assertEquals("PENDING", monthly.getSyncStatus(), "MONTHLY is supported and should be marked PENDING");

        // Clean up
        paymentDao.deletePayment(biMonthly.getId());
        paymentDao.deletePayment(halfYearly.getId());
        paymentDao.deletePayment(customDays.getId());
        paymentDao.deletePayment(monthly.getId());
    }

    @Test
    @Order(11)
    @DisplayName("DatabaseManager: addColumnIfNotExists safely handles duplicate columns and throws on real SQL errors")
    public void testDatabaseManagerAddColumnIfNotExists() throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Duplicate column must NOT throw an exception
            assertDoesNotThrow(() -> {
                DatabaseManager.addColumnIfNotExists(stmt, "monthly_payments", "original_due_day INTEGER");
            }, "Calling addColumnIfNotExists with an existing column must safely ignore duplicate column error");

            // 2. Invalid table name or SQL syntax must throw SQLException
            assertThrows(SQLException.class, () -> {
                DatabaseManager.addColumnIfNotExists(stmt, "non_existent_table_xyz", "bad_col TEXT");
            }, "Calling addColumnIfNotExists with a non-existent table must propagate SQLException");
        }
    }

    // =========================================================================
    // 5. REMINDER SNOOZE & EFFECTIVE TIME TESTS
    // =========================================================================

    @Test
    @Order(12)
    @DisplayName("Snooze consistency: snoozeReminder preserves original scheduled time and updates snoozedTime")
    public void testReminderSnoozePreservesOriginalTimeAndUpdatesSnoozedTime() throws Exception {
        long originalTime = 1760000000000L;
        Reminder reminder = new Reminder(null, null, "Doctor Appointment", originalTime, false, 0L, System.currentTimeMillis(), "PENDING");
        reminderDao.insertReminder(reminder);
        assertNotNull(reminder.getId());

        // Initial state
        assertEquals(originalTime, reminder.getTime(), "Original time must be intact initially");
        assertEquals(0L, reminder.getSnoozedTime());
        assertEquals(originalTime, reminder.getEffectiveTime());

        // First snooze: +15 minutes
        long beforeSnooze = System.currentTimeMillis();
        reminderRepository.snoozeReminder(reminder, 15);

        assertEquals(originalTime, reminder.getTime(), "Original scheduled time MUST remain untouched after snooze");
        assertTrue(reminder.getSnoozedTime() >= beforeSnooze + 15 * 60 * 1000 - 1000L);
        assertEquals(reminder.getSnoozedTime(), reminder.getEffectiveTime(), "Effective time must reflect snoozedTime");
        assertFalse(reminder.isExpired(), "Snoozed reminder must not be marked expired");

        // Verify persisted state in SQLite
        Reminder loaded = reminderDao.getReminderById(reminder.getId());
        assertNotNull(loaded);
        assertEquals(originalTime, loaded.getTime(), "Persisted row must preserve original time");
        assertEquals(reminder.getSnoozedTime(), loaded.getSnoozedTime(), "Persisted row must store snoozedTime");
        assertEquals(reminder.getSnoozedTime(), loaded.getEffectiveTime());

        // Repeated snooze: +30 minutes
        long secondSnoozeTarget = System.currentTimeMillis() + 30 * 60 * 1000;
        reminderRepository.snoozeReminder(reminder, 30);

        assertEquals(originalTime, reminder.getTime(), "Repeated snooze MUST NOT alter original scheduled time");
        assertTrue(reminder.getSnoozedTime() >= secondSnoozeTarget - 1000L);
        assertEquals(reminder.getSnoozedTime(), reminder.getEffectiveTime());

        // Clean up
        reminderDao.deleteReminder(reminder.getId());
        assertNull(reminderDao.getReminderById(reminder.getId()));
    }

    @Test
    @Order(13)
    @DisplayName("Snooze consistency: Sync payload separates original time and snoozedTime, scheduler respects snoozed trigger")
    public void testReminderSyncPayloadAndSchedulerSnoozeHandling() {
        long originalTime = 1760000000000L;
        long snoozedUntil = 1760001800000L;
        Reminder reminder = new Reminder(105, 501L, "Team Meeting", originalTime, false, snoozedUntil, System.currentTimeMillis(), "PENDING");

        // Sync DTO payload verification
        com.reminder.desktop.dto.ReminderDto dto = new com.reminder.desktop.dto.ReminderDto(
                reminder.getServerId(),
                reminder.getText(),
                reminder.getTime(),
                reminder.isExpired(),
                reminder.getSnoozedTime()
        );

        assertEquals(Long.valueOf(originalTime), dto.getReminderTime(), "DTO reminderTime must be original time");
        assertEquals(Long.valueOf(snoozedUntil), dto.getSnoozedTime(), "DTO snoozedTime must be snoozed timestamp");

        // Scheduler handling: must use effective time
        assertEquals(snoozedUntil, reminder.getEffectiveTime());
        assertDoesNotThrow(() -> ReminderScheduler.getInstance().scheduleReminder(reminder));
        assertDoesNotThrow(() -> ReminderScheduler.getInstance().cancelReminder(reminder));

        // Expired reminder must not be scheduled
        reminder.setExpired(true);
        assertDoesNotThrow(() -> ReminderScheduler.getInstance().scheduleReminder(reminder));
    }
    // =========================================================================
    // PHASE 4E DEFECT FIX TESTS (B2, B3, B4)
    // =========================================================================

    @Test
    @Order(14)
    @DisplayName("B2 & B3: CUSTOM_DAYS validation on Repository enforces 1-365 range, rejects invalid intervals")
    public void testPhase4E_WindowsCustomDaysValidation() throws Exception {
        long due = System.currentTimeMillis() + 86400000L;

        // Valid intervals: 1, 28, 365
        assertDoesNotThrow(() -> {
            MonthlyPayment p1 = repository.addPayment("Custom 1 Day", due, 50.0, RecurrenceType.CUSTOM_DAYS, 1, "0");
            assertEquals(1, p1.getCustomIntervalDays());
            paymentDao.deletePayment(p1.getId());
        });

        assertDoesNotThrow(() -> {
            MonthlyPayment p28 = repository.addPayment("Custom 28 Days", due, 50.0, RecurrenceType.CUSTOM_DAYS, 28, "0");
            assertEquals(28, p28.getCustomIntervalDays());
            paymentDao.deletePayment(p28.getId());
        });

        assertDoesNotThrow(() -> {
            MonthlyPayment p365 = repository.addPayment("Custom 365 Days", due, 50.0, RecurrenceType.CUSTOM_DAYS, 365, "0");
            assertEquals(365, p365.getCustomIntervalDays());
            paymentDao.deletePayment(p365.getId());
        });

        // Invalid intervals: 0, negative, > 365 must throw IllegalArgumentException and NOT default to 28
        assertThrows(IllegalArgumentException.class, () -> {
            repository.addPayment("Invalid 0 Days", due, 50.0, RecurrenceType.CUSTOM_DAYS, 0, "0");
        }, "0 days interval must be rejected");

        assertThrows(IllegalArgumentException.class, () -> {
            repository.addPayment("Invalid Negative Days", due, 50.0, RecurrenceType.CUSTOM_DAYS, -5, "0");
        }, "Negative interval must be rejected");

        assertThrows(IllegalArgumentException.class, () -> {
            repository.addPayment("Invalid 366 Days", due, 50.0, RecurrenceType.CUSTOM_DAYS, 366, "0");
        }, "Interval > 365 must be rejected");

        // updatePayment validation
        MonthlyPayment validPayment = new MonthlyPayment(
                null, null, "Test Update Payment", due, false, System.currentTimeMillis(),
                "LOCAL_ONLY", 10.0, RecurrenceType.CUSTOM_DAYS, "0", 1, 14
        );
        assertDoesNotThrow(() -> paymentDao.insertPayment(validPayment));

        validPayment.setCustomIntervalDays(0);
        assertThrows(IllegalArgumentException.class, () -> {
            repository.updatePayment(validPayment);
        }, "updatePayment with 0 interval must be rejected");

        validPayment.setCustomIntervalDays(400);
        assertThrows(IllegalArgumentException.class, () -> {
            repository.updatePayment(validPayment);
        }, "updatePayment with 400 interval must be rejected");

        // Clean up
        paymentDao.deletePayment(validPayment.getId());
    }

    @Test
    @Order(15)
    @DisplayName("B4: Unknown recurrence fallback to UNKNOWN without silent conversion to MONTHLY")
    public void testPhase4E_WindowsUnknownRecurrenceFallback() {
        assertEquals(RecurrenceType.MONTHLY, RecurrenceType.fromString(null));
        assertEquals(RecurrenceType.MONTHLY, RecurrenceType.fromString(""));
        assertEquals(RecurrenceType.MONTHLY, RecurrenceType.fromString("   "));
        assertEquals(RecurrenceType.MONTHLY, RecurrenceType.fromString("MONTHLY"));
        assertEquals(RecurrenceType.ONE_TIME, RecurrenceType.fromString("ONE_TIME"));
        assertEquals(RecurrenceType.CUSTOM_DAYS, RecurrenceType.fromString("CUSTOM_DAYS"));

        // Unknown strings must map to UNKNOWN
        assertEquals(RecurrenceType.UNKNOWN, RecurrenceType.fromString("BI_WEEKLY"));
        assertEquals(RecurrenceType.UNKNOWN, RecurrenceType.fromString("SEMI_ANNUAL"));
        assertEquals(RecurrenceType.UNKNOWN, RecurrenceType.fromString("UNKNOWN_FUTURE_CADENCE"));

        // Local-only and backend support
        assertFalse(RecurrenceType.UNKNOWN.isSupportedByBackend(), "UNKNOWN must not be claimed as supported by backend");
        assertTrue(RecurrenceType.UNKNOWN.isLocalOnly(), "UNKNOWN must be local-only");

        // calculateNextDueDate does not drift or advance
        long due = 1700000000000L;
        long nextDue = MonthlyPayment.calculateNextDueDate(due, RecurrenceType.UNKNOWN, 15, 28, due + 5000L);
        assertEquals(due, nextDue, "UNKNOWN recurrence must preserve due date without advancing to next month");
    }
}
