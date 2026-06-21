package com.reminder.desktop.database;

import com.reminder.desktop.models.MonthlyPayment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MonthlyPaymentDao {

    public void insertPayment(MonthlyPayment payment) throws SQLException {
        String sql = "INSERT INTO monthly_payments (server_id, name, due_date, completed, updated_at, sync_status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (payment.getServerId() != null) {
                pstmt.setLong(1, payment.getServerId());
            } else {
                pstmt.setNull(1, Types.BIGINT);
            }
            pstmt.setString(2, payment.getName());
            pstmt.setLong(3, payment.getDueDate());
            pstmt.setInt(4, payment.isCompleted() ? 1 : 0);
            pstmt.setLong(5, payment.getUpdatedAt() != null ? payment.getUpdatedAt() : System.currentTimeMillis());
            pstmt.setString(6, payment.getSyncStatus());
            
            pstmt.executeUpdate();
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    payment.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void updatePayment(MonthlyPayment payment) throws SQLException {
        String sql = "UPDATE monthly_payments SET server_id = ?, name = ?, due_date = ?, " +
                "completed = ?, updated_at = ?, sync_status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection()) {
            if (payment.getServerId() == null) {
                Long existingServerId = null;
                String checkSql = "SELECT server_id FROM monthly_payments WHERE id = ?";
                try (PreparedStatement checkPstmt = conn.prepareStatement(checkSql)) {
                    checkPstmt.setInt(1, payment.getId());
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
                    payment.setServerId(existingServerId);
                }
            }

            System.out.printf("[PAYMENT UPDATE LOG] localId=%d, serverId=%s, syncStatus=%s, updatedAt=%s%n",
                    payment.getId(), payment.getServerId(), payment.getSyncStatus(), payment.getUpdatedAt());

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                if (payment.getServerId() != null) {
                    pstmt.setLong(1, payment.getServerId());
                } else {
                    pstmt.setNull(1, Types.BIGINT);
                }
                pstmt.setString(2, payment.getName());
                pstmt.setLong(3, payment.getDueDate());
                pstmt.setInt(4, payment.isCompleted() ? 1 : 0);
                pstmt.setLong(5, payment.getUpdatedAt() != null ? payment.getUpdatedAt() : System.currentTimeMillis());
                pstmt.setString(6, payment.getSyncStatus());
                pstmt.setInt(7, payment.getId());
                
                pstmt.executeUpdate();
            }
        }
    }

    public void deletePayment(int id) throws SQLException {
        String sql = "DELETE FROM monthly_payments WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public void softDeletePayment(int id) throws SQLException {
        String sql = "UPDATE monthly_payments SET sync_status = 'DELETE_PENDING', updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public void clearAll() throws SQLException {
        String sql = "DELETE FROM monthly_payments";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    public MonthlyPayment getPaymentById(int id) throws SQLException {
        String sql = "SELECT * FROM monthly_payments WHERE id = ?";
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

    public MonthlyPayment getPaymentByServerId(long serverId) throws SQLException {
        String sql = "SELECT * FROM monthly_payments WHERE server_id = ?";
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

    public List<MonthlyPayment> getAllPayments() throws SQLException {
        List<MonthlyPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM monthly_payments WHERE sync_status IS NULL OR (sync_status != 'DELETE_PENDING' AND sync_status != 'DELETE_SYNCED') ORDER BY due_date ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                payments.add(mapRow(rs));
            }
        }
        return payments;
    }

    public List<MonthlyPayment> getDeletedPayments() throws SQLException {
        List<MonthlyPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM monthly_payments WHERE sync_status = 'DELETE_PENDING'";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                payments.add(mapRow(rs));
            }
        }
        return payments;
    }

    public List<MonthlyPayment> getSyncPendingPayments() throws SQLException {
        List<MonthlyPayment> payments = new ArrayList<>();
        String sql = "SELECT * FROM monthly_payments WHERE sync_status = 'PENDING'";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                payments.add(mapRow(rs));
            }
        }
        return payments;
    }

    private MonthlyPayment mapRow(ResultSet rs) throws SQLException {
        long serverIdVal = rs.getLong("server_id");
        Long serverId = rs.wasNull() ? null : serverIdVal;
        
        return new MonthlyPayment(
                rs.getInt("id"),
                serverId,
                rs.getString("name"),
                rs.getLong("due_date"),
                rs.getInt("completed") == 1,
                rs.getLong("updated_at"),
                rs.getString("sync_status")
        );
    }
}
