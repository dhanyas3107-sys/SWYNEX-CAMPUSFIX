package com.campusfix.dao;

import com.campusfix.model.*;

import java.sql.*;
import java.util.Optional;

/**
 * SQL for the {@code users} table. Every value is bound through a PreparedStatement.
 * Methods receive a Connection so the caller (a service) controls the transaction.
 */
public class UserDAO {

    private static final String SELECT_USER =
            "SELECT u.user_id, u.name, u.email, u.password_hash, u.phone_number, u.role, u.is_active, u.created_at, "
                    + "t.technician_id, t.specialization, t.contact_info, t.availability "
                    + "FROM users u LEFT JOIN technicians t ON t.user_id = u.user_id ";

    /** Inserts the user and stores the generated id on the object. */
    public int insert(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (name, email, password_hash, phone_number, role, is_active) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getPhoneNumber());
            ps.setString(5, user.getRole().name());
            ps.setBoolean(6, user.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                user.setUserId(keys.getInt(1));
                return user.getUserId();
            }
        }
    }

    public Optional<User> findByEmail(Connection conn, String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_USER + "WHERE u.email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findById(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_USER + "WHERE u.user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean existsByEmail(Connection conn, String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /** @return true if at least one ACTIVE administrator account exists */
    public boolean existsActiveAdministrator(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM users WHERE role = 'ADMINISTRATOR' AND is_active = 1");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    public boolean setActive(Connection conn, int userId, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE users SET is_active = ? WHERE user_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    private User map(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String hash = rs.getString("password_hash");
        String phone = rs.getString("phone_number");
        Timestamp created = rs.getTimestamp("created_at");
        var createdAt = created == null ? null : created.toLocalDateTime();
        Role role = Role.valueOf(rs.getString("role"));

        User user = switch (role) {
            case STUDENT -> new Student(id, name, email, hash, phone, createdAt);
            case FACULTY -> new Faculty(id, name, email, hash, phone, createdAt);
            case ADMINISTRATOR -> new Administrator(id, name, email, hash, phone, createdAt);
            case TECHNICIAN -> new Technician(id, name, email, hash, phone, createdAt,
                    rs.getInt("technician_id"), Category.valueOf(rs.getString("specialization")),
                    rs.getString("contact_info"), Availability.valueOf(rs.getString("availability")));
        };
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }
}
