package com.reminder.desktop.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL;
    private static boolean initialized = false;

    static {
        String dbPath = "reminder.db";
        try {
            String appData = System.getenv("LOCALAPPDATA");
            if (appData == null || appData.trim().isEmpty()) {
                appData = System.getProperty("user.home");
            }
            File dbDir = new File(appData, "Reminder");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            dbPath = new File(dbDir, "reminder.db").getAbsolutePath();
        } catch (Exception e) {
            System.err.println("Could not resolve database path: " + e.getMessage());
        }
        DB_URL = "jdbc:sqlite:" + dbPath + "?journal_mode=WAL&busy_timeout=5000";
    }

    public static synchronized Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC Driver not found", e);
        }
        Connection conn = DriverManager.getConnection(DB_URL);
        if (!initialized) {
            initializeTables(conn);
            initialized = true;
        }
        return conn;
    }

    public static void initializeTables(Connection conn) throws SQLException {
        String createNotesTable = "CREATE TABLE IF NOT EXISTS quick_notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "server_id BIGINT, " +
                "text TEXT NOT NULL, " +
                "is_completed INTEGER DEFAULT 0, " +
                "position INTEGER DEFAULT 0, " +
                "updated_at BIGINT, " +
                "sync_status TEXT" +
                ");";

        String createRemindersTable = "CREATE TABLE IF NOT EXISTS reminders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "server_id BIGINT, " +
                "text TEXT, " +
                "time BIGINT, " +
                "is_expired INTEGER DEFAULT 0, " +
                "snoozed_time BIGINT DEFAULT 0, " +
                "updated_at BIGINT, " +
                "sync_status TEXT" +
                ");";

        String createPaymentsTable = "CREATE TABLE IF NOT EXISTS monthly_payments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "server_id BIGINT, " +
                "name TEXT, " +
                "due_date BIGINT, " +
                "completed INTEGER DEFAULT 0, " +
                "updated_at BIGINT, " +
                "sync_status TEXT, " +
                "amount REAL, " +
                "recurrence TEXT DEFAULT 'MONTHLY', " +
                "notification_offsets TEXT DEFAULT '0', " +
                "last_completed_month INTEGER, " +
                "last_completed_year INTEGER, " +
                "completed_at INTEGER, " +
                "last_paid_at INTEGER, " +
                "original_due_day INTEGER, " +
                "custom_interval_days INTEGER" +
                ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createNotesTable);
            stmt.execute(createRemindersTable);
            stmt.execute(createPaymentsTable);

            addColumnIfNotExists(stmt, "monthly_payments", "amount REAL");
            addColumnIfNotExists(stmt, "monthly_payments", "recurrence TEXT DEFAULT 'MONTHLY'");
            addColumnIfNotExists(stmt, "monthly_payments", "notification_offsets TEXT DEFAULT '0'");
            addColumnIfNotExists(stmt, "monthly_payments", "last_completed_month INTEGER");
            addColumnIfNotExists(stmt, "monthly_payments", "last_completed_year INTEGER");
            addColumnIfNotExists(stmt, "monthly_payments", "completed_at INTEGER");
            addColumnIfNotExists(stmt, "monthly_payments", "last_paid_at INTEGER");
            addColumnIfNotExists(stmt, "monthly_payments", "original_due_day INTEGER");
            addColumnIfNotExists(stmt, "monthly_payments", "custom_interval_days INTEGER");
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
            throw e;
        }
    }

    public static void addColumnIfNotExists(Statement stmt, String tableName, String colDef) throws SQLException {
        try {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + colDef);
        } catch (SQLException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate column")) {
                return;
            }
            throw e;
        }
    }
}
