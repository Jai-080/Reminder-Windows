package com.reminder.desktop.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:reminder.db?journal_mode=WAL&busy_timeout=5000";
    private static boolean initialized = false;

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

    private static void initializeTables(Connection conn) {
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
                "notification_offsets TEXT DEFAULT '0'" +
                ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createNotesTable);
            stmt.execute(createRemindersTable);
            stmt.execute(createPaymentsTable);
            try {
                stmt.execute("ALTER TABLE monthly_payments ADD COLUMN amount REAL");
            } catch (SQLException e) {
                // Column already exists
            }
            try {
                stmt.execute("ALTER TABLE monthly_payments ADD COLUMN recurrence TEXT DEFAULT 'MONTHLY'");
            } catch (SQLException e) {
                // Column already exists
            }
            try {
                stmt.execute("ALTER TABLE monthly_payments ADD COLUMN notification_offsets TEXT DEFAULT '0'");
            } catch (SQLException e) {
                // Column already exists
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
        }
    }
}
