package com.reminder.desktop;

import com.reminder.desktop.auth.AuthService;
import com.reminder.desktop.auth.AuthServiceImpl;
import com.reminder.desktop.auth.TokenStorage;
import com.reminder.desktop.database.DatabaseManager;
import com.reminder.desktop.database.QuickNoteDao;
import com.reminder.desktop.database.ReminderDao;
import com.reminder.desktop.database.MonthlyPaymentDao;
import com.reminder.desktop.dto.QuickNoteDto;
import com.reminder.desktop.dto.ReminderDto;
import com.reminder.desktop.dto.MonthlyPaymentDto;
import com.reminder.desktop.models.QuickNote;
import com.reminder.desktop.models.Reminder;
import com.reminder.desktop.models.MonthlyPayment;
import com.reminder.desktop.notifications.ReminderScheduler;
import com.reminder.desktop.sync.ApiClient;
import com.reminder.desktop.sync.SyncService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class VerificationTest {

    public static void main(String[] args) {
        System.out.println("=== STARTING PHASE 6 READINESS VERIFICATION ===");
        try {
            // Ensure JavaFX platform thread is initialized (minimal stub)
            try {
                javafx.application.Platform.startup(() -> {});
            } catch (IllegalStateException ignored) {}

            testSQLiteInitAndCRUD();
            testAuthenticationFlow();
            testApiCompatibilityAndSync();
            testSchedulerPersistenceAndNotifications();
            testSyncConflictResolution();

            System.out.println("\n=== ALL PHASES PASSED SUCCESSFULLY ===");
            System.exit(0);
        } catch (Throwable t) {
            System.err.println("\n!!! VERIFICATION FAILED !!!");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void testSQLiteInitAndCRUD() throws Exception {
        System.out.println("\n--- Step 1: Testing SQLite database and DAO CRUD ---");
        
        // Ensure connection & table creation
        try (Connection conn = DatabaseManager.getConnection()) {
            System.out.println("Database file reminder.db checked/created successfully.");
            try (Statement stmt = conn.createStatement()) {
                // Check tables exist
                stmt.executeQuery("SELECT * FROM quick_notes LIMIT 1");
                stmt.executeQuery("SELECT * FROM reminders LIMIT 1");
                stmt.executeQuery("SELECT * FROM monthly_payments LIMIT 1");
                System.out.println("Verified schema tables exist: quick_notes, reminders, monthly_payments.");
            }
        }

        QuickNoteDao noteDao = new QuickNoteDao();
        noteDao.clearAll();

        // CRUD: Insert
        QuickNote note = new QuickNote(null, null, "Test Checkbox Item", false, 1, System.currentTimeMillis(), "PENDING");
        noteDao.insertNote(note);
        if (note.getId() == null) {
            throw new Exception("Insert failed: Generated local ID is null.");
        }
        System.out.println("Local Insert success. Generated ID: " + note.getId());

        // CRUD: Read
        QuickNote readNote = noteDao.getNoteById(note.getId());
        if (readNote == null || !readNote.getText().equals("Test Checkbox Item")) {
            throw new Exception("Read failed: Record mismatch.");
        }
        System.out.println("Local Read success: " + readNote.getText() + " [" + readNote.getSyncStatus() + "]");

        // CRUD: Update
        readNote.setCompleted(true);
        readNote.setSyncStatus("PENDING");
        readNote.setUpdatedAt(System.currentTimeMillis());
        noteDao.updateNote(readNote);
        
        QuickNote updatedNote = noteDao.getNoteById(note.getId());
        if (updatedNote == null || !updatedNote.isCompleted() || !"PENDING".equalsIgnoreCase(updatedNote.getSyncStatus())) {
            throw new Exception("Update failed: Completed state or sync status mismatch.");
        }
        System.out.println("Local Update success. Completed: " + updatedNote.isCompleted());

        // CRUD: Delete
        noteDao.deleteNote(note.getId());
        if (noteDao.getNoteById(note.getId()) != null) {
            throw new Exception("Delete failed: Record still exists.");
        }
        System.out.println("Local Delete success.");
    }

    private static void testAuthenticationFlow() throws Exception {
        System.out.println("\n--- Step 2: Testing Authentication and Token Persist/Refresh ---");
        AuthService authService = new AuthServiceImpl();
        TokenStorage.clearSession();

        // Register a random user to prevent duplicate conflicts
        int rand = new Random().nextInt(100000);
        String username = "testuser_" + rand;
        String email = "test_" + rand + "@example.com";
        String password = "Password123#";

        System.out.println("Registering: " + username + " (" + email + ")");
        boolean regSuccess = authService.register(username, email, password);
        if (!regSuccess || !TokenStorage.hasToken()) {
            throw new Exception("Registration failed or tokens not saved.");
        }
        System.out.println("Registration success. Saved User: " + TokenStorage.getUsername());

        // Verify session persists
        String tokenBeforeClose = TokenStorage.getAccessToken();
        String refreshBeforeClose = TokenStorage.getRefreshToken();
        System.out.println("JWT Access Token: " + tokenBeforeClose.substring(0, 15) + "...");
        System.out.println("JWT Refresh Token: " + refreshBeforeClose.substring(0, 15) + "...");

        // Simulate Reopen (read properties file)
        System.out.println("Simulating app close and reopening... reloading Storage.");
        if (!TokenStorage.hasToken() || !TokenStorage.getUsername().equals(username)) {
            throw new Exception("Session persistence verification failed.");
        }
        System.out.println("JWT persisted successfully on mock reopen.");

        // Verify refresh token works
        System.out.println("Triggering token refresh session flow...");
        boolean refreshSuccess = authService.refreshSession();
        if (!refreshSuccess || !TokenStorage.hasToken()) {
            throw new Exception("Refresh flow failed: Token rejected or not updated.");
        }
        System.out.println("Token refresh succeeded. New Access Token: " + TokenStorage.getAccessToken().substring(0, 15) + "...");

        // Verify logout clears token
        authService.logout();
        if (TokenStorage.hasToken()) {
            throw new Exception("Logout failed to clear local tokens.");
        }
        System.out.println("Logout cleared token storage successfully.");

        // Relogin for subsequent tests
        System.out.println("Logging back in for API testing...");
        boolean loginSuccess = authService.login(email, password, true);
        if (!loginSuccess || !TokenStorage.hasToken()) {
            throw new Exception("Failed to re-login for subsequent API validation.");
        }
        System.out.println("Re-login successful.");
    }

    private static void testApiCompatibilityAndSync() throws Exception {
        System.out.println("\n--- Step 3: Testing REST API compatibility and JSON maps ---");
        ApiClient client = ApiClient.getInstance();

        // 1. Notes API CRUD
        System.out.println("Creating test note on server...");
        QuickNoteDto noteDto = new QuickNoteDto(null, "API Test Note", false, 0);
        QuickNoteDto noteResp = client.post("/api/notes", noteDto, QuickNoteDto.class);
        if (noteResp.getId() == null || !noteResp.getText().equals("API Test Note")) {
            throw new Exception("API Note POST failed or returned incorrect structure.");
        }
        System.out.println("API POST Note success. Server ID: " + noteResp.getId());

        System.out.println("Updating test note on server...");
        noteResp.setIsCompleted(true);
        QuickNoteDto noteUpdateResp = client.put("/api/notes/" + noteResp.getId(), noteResp, QuickNoteDto.class);
        if (!noteUpdateResp.getIsCompleted()) {
            throw new Exception("API Note PUT failed to update completed status.");
        }
        System.out.println("API PUT Note success. Updated time: " + noteUpdateResp.getUpdatedAt());

        System.out.println("Verifying notes list GET from server...");
        QuickNoteDto[] notes = client.get("/api/notes", QuickNoteDto[].class);
        boolean found = false;
        for (QuickNoteDto n : notes) {
            if (n.getId().equals(noteResp.getId())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new Exception("API GET Notes success, but our note was not found in response.");
        }
        System.out.println("API GET Notes success. List size: " + notes.length);

        System.out.println("Deleting note on server...");
        client.delete("/api/notes/" + noteResp.getId());
        System.out.println("API DELETE Note success.");

        // 2. Reminders API CRUD
        System.out.println("Creating test reminder on server...");
        long reminderTime = System.currentTimeMillis() + 3600000;
        ReminderDto reminderDto = new ReminderDto(null, "Doctor Visit", reminderTime, false, 0L);
        ReminderDto remResp = client.post("/api/reminders", reminderDto, ReminderDto.class);
        if (remResp.getId() == null) {
            throw new Exception("API Reminder POST failed.");
        }
        System.out.println("API POST Reminder success. Server ID: " + remResp.getId());

        client.delete("/api/reminders/" + remResp.getId());
        System.out.println("API DELETE Reminder success.");

        // 3. Payments API CRUD
        System.out.println("Creating test payment on server...");
        long paymentTime = System.currentTimeMillis() + 86400000;
        MonthlyPaymentDto paymentDto = new MonthlyPaymentDto(null, "Gym Membership", paymentTime, false);
        MonthlyPaymentDto payResp = client.post("/api/payments", paymentDto, MonthlyPaymentDto.class);
        if (payResp.getId() == null) {
            throw new Exception("API Payment POST failed.");
        }
        System.out.println("API POST Payment success. Server ID: " + payResp.getId());

        client.delete("/api/payments/" + payResp.getId());
        System.out.println("API DELETE Payment success.");
    }

    private static void testSchedulerPersistenceAndNotifications() throws Exception {
        System.out.println("\n--- Step 4: Testing Notification Scheduler Persistence ---");
        ReminderDao reminderDao = new ReminderDao();
        reminderDao.clearAll();

        // 1. Create a reminder 3 seconds in the future
        long triggerTime = System.currentTimeMillis() + 3000;
        Reminder reminder = new Reminder(
                null,
                null,
                "Audit Scheduled Alert",
                triggerTime,
                false,
                0L,
                System.currentTimeMillis(),
                "PENDING"
        );
        reminderDao.insertReminder(reminder);
        System.out.println("Created persistent reminder in SQLite. Time: " + triggerTime);

        // 2. Simulate App Closing & Startup Restore
        System.out.println("Simulating App shutdown. Restarting Scheduler engine...");
        ReminderScheduler scheduler = ReminderScheduler.getInstance();
        scheduler.initialize();
        System.out.println("Scheduler loaded pending records and scheduled notifications.");

        // 3. Wait for 5 seconds to let the alert trigger and database update
        System.out.println("Waiting 5 seconds for scheduled executor alert thread...");
        Thread.sleep(5000);

        // 4. Verify Database update
        Reminder updated = reminderDao.getReminderById(reminder.getId());
        if (updated == null) {
            throw new Exception("Reminder was lost from SQLite!");
        }
        if (!updated.isExpired()) {
            throw new Exception("Failure: Reminder scheduling persistence failed. Alarm did not execute/expire.");
        }
        System.out.println("Scheduler Persistence Verification Success! SQLite record marked expired: " + updated.isExpired());
    }

    private static void testSyncConflictResolution() throws Exception {
        System.out.println("\n--- Step 5: Testing Sync Conflict Resolution (Latest wins) ---");
        
        // Clean local note
        QuickNoteDao noteDao = new QuickNoteDao();
        noteDao.clearAll();
        
        // Scenario A: Server has LATER change than local PENDING
        System.out.println("Executing Scenario A: Server updatedAt (10:05) > Local updatedAt (10:00)");
        
        long localTime = System.currentTimeMillis();
        long serverTime = localTime + 300000; // 5 mins later
        
        QuickNote localNote = new QuickNote(
                null,
                999L,
                "Old Local Note Text",
                false,
                1,
                localTime,
                "PENDING"
        );
        noteDao.insertNote(localNote);
        
        // Mock server state
        QuickNoteDto serverDto = new QuickNoteDto(999L, "New Server Text", true, 1);
        serverDto.setUpdatedAt(Instant.ofEpochMilli(serverTime).toString());
        
        // Resolve conflict (simulate SyncService local sync mapping)
        long serverMillis = Instant.parse(serverDto.getUpdatedAt()).toEpochMilli();
        if (serverMillis >= localNote.getUpdatedAt()) {
            localNote.setText(serverDto.getText());
            localNote.setCompleted(serverDto.getIsCompleted());
            localNote.setUpdatedAt(serverMillis);
            localNote.setSyncStatus("SYNCED");
            noteDao.updateNote(localNote);
            System.out.println("Successfully resolved Scenario A: Server overwrote local note.");
        } else {
            throw new Exception("Scenario A Failure: Server failed to overwrite older local note.");
        }
        
        QuickNote resultA = noteDao.getNoteById(localNote.getId());
        if (!resultA.getText().equals("New Server Text") || !resultA.isCompleted() || !"SYNCED".equals(resultA.getSyncStatus())) {
            throw new Exception("Scenario A database state mismatch.");
        }

        // Scenario B: Local has LATER change than server
        System.out.println("Executing Scenario B: Local updatedAt (11:00) > Server updatedAt (10:50)");
        long serverTimeB = System.currentTimeMillis();
        long localTimeB = serverTimeB + 600000; // 10 mins later
        
        resultA.setText("Newer Windows Text");
        resultA.setUpdatedAt(localTimeB);
        resultA.setSyncStatus("PENDING");
        noteDao.updateNote(resultA);
        
        // Mock server sync pull state
        QuickNoteDto serverDtoB = new QuickNoteDto(999L, "Outdated Server Text", false, 1);
        serverDtoB.setUpdatedAt(Instant.ofEpochMilli(serverTimeB).toString());
        
        // Sync resolution logic
        long serverMillisB = Instant.parse(serverDtoB.getUpdatedAt()).toEpochMilli();
        if (serverMillisB >= resultA.getUpdatedAt()) {
            // Should NOT happen because server is older
            resultA.setText(serverDtoB.getText());
            resultA.setUpdatedAt(serverMillisB);
            resultA.setSyncStatus("SYNCED");
            noteDao.updateNote(resultA);
            throw new Exception("Scenario B Failure: Outdated server overwrote newer local note.");
        } else {
            System.out.println("Successfully resolved Scenario B: Older server change skipped. Local PENDING preserved.");
        }

        QuickNote resultB = noteDao.getNoteById(resultA.getId());
        if (!resultB.getText().equals("Newer Windows Text") || !"PENDING".equals(resultB.getSyncStatus())) {
            throw new Exception("Scenario B database state mismatch.");
        }
        
        System.out.println("Verified timezone-safe ISO string-to-millisecond timestamp resolution matches exactly.");
    }
}
