package com.campusfix.dao;

import com.campusfix.model.*;

import java.sql.*;
import java.util.*;

/** SQL for the {@code technicians} and {@code complaint_assignments} tables. */
public class TechnicianDAO {

    private static final String SELECT_TECHNICIAN =
            "SELECT t.technician_id, t.user_id, u.name, u.email, u.phone_number, u.created_at, u.is_active, "
                    + "t.specialization, t.contact_info, t.availability "
                    + "FROM technicians t JOIN users u ON u.user_id = t.user_id ";

    /** Inserts the technician row for a user that already exists; stores the new technician id on the object. */
    public int insert(Connection conn, Technician technician) throws SQLException {
        String sql = "INSERT INTO technicians (user_id, specialization, contact_info, availability) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, technician.getUserId());
            ps.setString(2, technician.getSpecialization().name());
            ps.setString(3, technician.getContactInfo());
            ps.setString(4, technician.getAvailability().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                technician.setTechnicianId(keys.getInt(1));
                return technician.getTechnicianId();
            }
        }
    }

    public List<Technician> findAll(Connection conn) throws SQLException {
        List<Technician> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_TECHNICIAN + "ORDER BY u.name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(map(rs));
            }
        }
        return result;
    }

    public Optional<Technician> findById(Connection conn, int technicianId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_TECHNICIAN + "WHERE t.technician_id = ?")) {
            ps.setInt(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public boolean updateAvailability(Connection conn, int technicianId, Availability availability) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE technicians SET availability = ? WHERE technician_id = ?")) {
            ps.setString(1, availability.name());
            ps.setInt(2, technicianId);
            return ps.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------------ assignments

    public void insertAssignment(Connection conn, int complaintId, int technicianId, int assignedBy) throws SQLException {
        String sql = "INSERT INTO complaint_assignments (complaint_id, technician_id, assigned_by) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            ps.setInt(2, technicianId);
            ps.setInt(3, assignedBy);
            ps.executeUpdate();
        }
    }

    /** Ends the current active assignment of a complaint (used when it is reassigned). */
    public int releaseActiveAssignment(Connection conn, int complaintId) throws SQLException {
        String sql = "UPDATE complaint_assignments SET is_active = 0, released_at = CURRENT_TIMESTAMP "
                + "WHERE complaint_id = ? AND is_active = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            return ps.executeUpdate();
        }
    }

    public int markAccepted(Connection conn, int complaintId, int technicianId) throws SQLException {
        String sql = "UPDATE complaint_assignments SET accepted_at = CURRENT_TIMESTAMP "
                + "WHERE complaint_id = ? AND technician_id = ? AND is_active = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            ps.setInt(2, technicianId);
            return ps.executeUpdate();
        }
    }

    /** Closes the active assignment as completed. */
    public int markCompleted(Connection conn, int complaintId, int technicianId) throws SQLException {
        String sql = "UPDATE complaint_assignments SET completed_at = CURRENT_TIMESTAMP, is_active = 0 "
                + "WHERE complaint_id = ? AND technician_id = ? AND is_active = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            ps.setInt(2, technicianId);
            return ps.executeUpdate();
        }
    }

    public int countActiveAssignments(Connection conn, int technicianId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM complaint_assignments WHERE technician_id = ? AND is_active = 1")) {
            ps.setInt(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** @return technicianId -> number of active assignments (technicians with none are absent) */
    public Map<Integer, Integer> activeAssignmentCounts(Connection conn) throws SQLException {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT technician_id, COUNT(*) AS active_count FROM complaint_assignments "
                + "WHERE is_active = 1 GROUP BY technician_id";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                counts.put(rs.getInt("technician_id"), rs.getInt("active_count"));
            }
        }
        return counts;
    }

    public int countCompleted(Connection conn, int technicianId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM complaint_assignments WHERE technician_id = ? AND completed_at IS NOT NULL")) {
            ps.setInt(1, technicianId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private Technician map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        // The password hash is intentionally NOT loaded here; technicians listed by admins never need it.
        Technician t = new Technician(rs.getInt("user_id"), rs.getString("name"), rs.getString("email"), "",
                rs.getString("phone_number"), created == null ? null : created.toLocalDateTime(),
                rs.getInt("technician_id"), Category.valueOf(rs.getString("specialization")),
                rs.getString("contact_info"), Availability.valueOf(rs.getString("availability")));
        t.setActive(rs.getBoolean("is_active"));
        return t;
    }
}
