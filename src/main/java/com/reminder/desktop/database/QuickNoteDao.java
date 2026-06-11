package com.reminder.desktop.database;

import com.reminder.desktop.models.QuickNote;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuickNoteDao {

    public void insertNote(QuickNote note) throws SQLException {
        String sql = "INSERT INTO quick_notes (server_id, text, is_completed, position, updated_at, sync_status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (note.getServerId() != null) {
                pstmt.setLong(1, note.getServerId());
            } else {
                pstmt.setNull(1, Types.BIGINT);
            }
            pstmt.setString(2, note.getText());
            pstmt.setInt(3, note.isCompleted() ? 1 : 0);
            pstmt.setInt(4, note.getPosition());
            pstmt.setLong(5, note.getUpdatedAt() != null ? note.getUpdatedAt() : System.currentTimeMillis());
            pstmt.setString(6, note.getSyncStatus());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    note.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void updateNote(QuickNote note) throws SQLException {
        String sql = "UPDATE quick_notes SET server_id = ?, text = ?, is_completed = ?, position = ?, " +
                "updated_at = ?, sync_status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (note.getServerId() != null) {
                pstmt.setLong(1, note.getServerId());
            } else {
                pstmt.setNull(1, Types.BIGINT);
            }
            pstmt.setString(2, note.getText());
            pstmt.setInt(3, note.isCompleted() ? 1 : 0);
            pstmt.setInt(4, note.getPosition());
            pstmt.setLong(5, note.getUpdatedAt() != null ? note.getUpdatedAt() : System.currentTimeMillis());
            pstmt.setString(6, note.getSyncStatus());
            pstmt.setInt(7, note.getId());
            
            pstmt.executeUpdate();
        }
    }

    public void deleteNote(int id) throws SQLException {
        String sql = "DELETE FROM quick_notes WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public void clearAll() throws SQLException {
        String sql = "DELETE FROM quick_notes";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public QuickNote getNoteById(int id) throws SQLException {
        String sql = "SELECT * FROM quick_notes WHERE id = ?";
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

    public QuickNote getNoteByServerId(long serverId) throws SQLException {
        String sql = "SELECT * FROM quick_notes WHERE server_id = ?";
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

    public List<QuickNote> getAllNotes() throws SQLException {
        List<QuickNote> notes = new ArrayList<>();
        String sql = "SELECT * FROM quick_notes ORDER BY position ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                notes.add(mapRow(rs));
            }
        }
        return notes;
    }

    public List<QuickNote> getPendingNotes() throws SQLException {
        List<QuickNote> notes = new ArrayList<>();
        String sql = "SELECT * FROM quick_notes WHERE sync_status = 'PENDING'";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                notes.add(mapRow(rs));
            }
        }
        return notes;
    }

    public int getMaxPosition() throws SQLException {
        String sql = "SELECT MAX(position) FROM quick_notes";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private QuickNote mapRow(ResultSet rs) throws SQLException {
        long serverIdVal = rs.getLong("server_id");
        Long serverId = rs.wasNull() ? null : serverIdVal;
        
        return new QuickNote(
                rs.getInt("id"),
                serverId,
                rs.getString("text"),
                rs.getInt("is_completed") == 1,
                rs.getInt("position"),
                rs.getLong("updated_at"),
                rs.getString("sync_status")
        );
    }
}
