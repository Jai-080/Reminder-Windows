package com.reminder.desktop.database;

import com.reminder.desktop.models.Reminder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReminderDao {

    public void insertReminder(Reminder reminder) throws SQLException {
        String sql = "INSERT INTO reminders (server_id, text, time, is_expired, snoozed_time, updated_at, sync_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (reminder.getServerId() != null) {
                pstmt.setLong(1, reminder.getServerId());
            } else {
                pstmt.setNull(1, Types.BIGINT);
            }
            pstmt.setString(2, reminder.getText());
            pstmt.setLong(3, reminder.getTime());
            pstmt.setInt(4, reminder.isExpired() ? 1 : 0);
            pstmt.setLong(5, reminder.getSnoozedTime());
            pstmt.setLong(6, reminder.getUpdatedAt() != null ? reminder.getUpdatedAt() : System.currentTimeMillis());
            pstmt.setString(7, reminder.getSyncStatus());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    reminder.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void updateReminder(Reminder reminder) throws SQLException {
        String sql = "UPDATE reminders SET server_id = ?, text = ?, time = ?, is_expired = ?, " +
                "snoozed_time = ?, updated_at = ?, sync_status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            if (reminder.getServerId() == null) {
                Long existingServerId = null;
                String checkSql = "SELECT server_id FROM reminders WHERE id = ?";
                try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                    checkPstmt.setInt(1, reminder.getId());
                    try (ResultSet rs = checkPstmt.executeQuery()) {
                        if (rs.next()) {
                            long sid = rs.getLong("server_id");
                            if (!rs.wasNull()) {
                                existingServerId = sid;
                            }
                        }
                    }
                }
                if (existingServerId != null) {
                    reminder.setServerId(existingServerId);
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                if (reminder.getServerId() != null) {
                    pstmt.setLong(1, reminder.getServerId());
                } else {
                    pstmt.setNull(1, Types.BIGINT);
                }
                pstmt.setString(2, reminder.getText());
                pstmt.setLong(3, reminder.getTime());
                pstmt.setInt(4, reminder.isExpired() ? 1 : 0);
                pstmt.setLong(5, reminder.getSnoozedTime());
                pstmt.setLong(6, reminder.getUpdatedAt() != null ? reminder.getUpdatedAt() : System.currentTimeMillis());
                pstmt.setString(7, reminder.getSyncStatus());
                pstmt.setInt(8, reminder.getId());
                
                pstmt.executeUpdate();
            }
        }
    }

    public void deleteReminder(int id) throws SQLException {
        String sql = "DELETE FROM reminders WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public void clearAll() throws SQLException {
        String sql = "DELETE FROM reminders";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public Reminder getReminderById(int id) throws SQLException {
        String sql = "SELECT * FROM reminders WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Reminder getReminderByServerId(long serverId) throws SQLException {
        String sql = "SELECT * FROM reminders WHERE server_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, serverId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Reminder> getAllReminders() throws SQLException {
        List<Reminder> reminders = new ArrayList<>();
        String sql = "SELECT * FROM reminders ORDER BY time ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reminders.add(mapRow(rs));
            }
        }
        return reminders;
    }

    public List<Reminder> getPendingReminders() throws SQLException {
        List<Reminder> reminders = new ArrayList<>();
        String sql = "SELECT * FROM reminders WHERE is_expired = 0 ORDER BY time ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reminders.add(mapRow(rs));
            }
        }
        return reminders;
    }

    public List<Reminder> getExpiredReminders() throws SQLException {
        List<Reminder> reminders = new ArrayList<>();
        String sql = "SELECT * FROM reminders WHERE is_expired = 1 ORDER BY time ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reminders.add(mapRow(rs));
            }
        }
        return reminders;
    }

    public List<Reminder> getSyncPendingReminders() throws SQLException {
        List<Reminder> reminders = new ArrayList<>();
        String sql = "SELECT * FROM reminders WHERE sync_status = 'PENDING'";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reminders.add(mapRow(rs));
            }
        }
        return reminders;
    }

    private Reminder mapRow(ResultSet rs) throws SQLException {
        long serverIdVal = rs.getLong("server_id");
        Long serverId = rs.wasNull() ? null : serverIdVal;
        
        return new Reminder(
                rs.getInt("id"),
                serverId,
                rs.getString("text"),
                rs.getLong("time"),
                rs.getInt("is_expired") == 1,
                rs.getLong("snoozed_time"),
                rs.getLong("updated_at"),
                rs.getString("sync_status")
        );
    }
}
