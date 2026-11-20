package com.campusfix.dao;

import com.campusfix.model.*;

import java.sql.*;
import java.util.*;

/**
 * SQL for the {@code complaints} table.
 *
 * <p>Dynamic filters only append fixed SQL fragments containing '?' placeholders; the values
 * themselves are always bound with the PreparedStatement, so SQL injection is not possible.
 */
public class ComplaintDAO {

    private static final String SELECT_BASE =
            "SELECT c.complaint_id, c.submitted_by, su.name AS submitter_name, c.category, c.description, "
                    + "c.building, c.location, c.priority, c.status, c.assigned_technician_id, "
                    + "tu.name AS technician_name, c.created_at, c.updated_at, c.resolved_at "
                    + "FROM complaints c "
                    + "JOIN users su ON su.user_id = c.submitted_by "
                    + "LEFT JOIN technicians t ON t.technician_id = c.assigned_technician_id "
                    + "LEFT JOIN users tu ON tu.user_id = t.user_id ";

    private static final String ORDER_NEWEST_FIRST = " ORDER BY c.created_at DESC, c.complaint_id DESC";

    public int insert(Connection conn, Complaint complaint) throws SQLException {
        String sql = "INSERT INTO complaints (submitted_by, category, description, building, location, priority, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, complaint.getSubmittedBy());
            ps.setString(2, complaint.getCategory().name());
            ps.setString(3, complaint.getDescription());
            ps.setString(4, complaint.getBuilding());
            ps.setString(5, complaint.getLocation());
            ps.setString(6, complaint.getPriority().name());
            ps.setString(7, complaint.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                complaint.setComplaintId(keys.getInt(1));
                return complaint.getComplaintId();
            }
        }
    }

    /**
     * @param lockForUpdate when true the row is locked until the transaction ends (prevents two
     *                      administrators from changing the same complaint at the same time)
     */
    public Optional<Complaint> findById(Connection conn, int complaintId, boolean lockForUpdate) throws SQLException {
        String sql = SELECT_BASE + "WHERE c.complaint_id = ?" + (lockForUpdate ? " FOR UPDATE OF c" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Complaint> findAll(Connection conn) throws SQLException {
        return query(conn, SELECT_BASE + ORDER_NEWEST_FIRST, List.of());
    }

    public List<Complaint> findBySubmitter(Connection conn, int userId) throws SQLException {
        return query(conn, SELECT_BASE + "WHERE c.submitted_by = ?" + ORDER_NEWEST_FIRST, List.of(userId));
    }

    public List<Complaint> findByStatuses(Connection conn, Set<ComplaintStatus> statuses) throws SQLException {
        List<Object> params = new ArrayList<>(statuses.stream().map(Enum::name).toList());
        return query(conn, SELECT_BASE + "WHERE c.status IN (" + placeholders(statuses.size()) + ")"
                + ORDER_NEWEST_FIRST, params);
    }

    public List<Complaint> findByTechnician(Connection conn, int technicianId, Set<ComplaintStatus> statuses)
            throws SQLException {
        List<Object> params = new ArrayList<>();
        params.add(technicianId);
        statuses.forEach(s -> params.add(s.name()));
        return query(conn, SELECT_BASE + "WHERE c.assigned_technician_id = ? AND c.status IN ("
                + placeholders(statuses.size()) + ")" + ORDER_NEWEST_FIRST, params);
    }

    /** Keyword search over description, building, location, submitter name and complaint id. */
    public List<Complaint> search(Connection conn, String keyword) throws SQLException {
        String like = "%" + escapeLike(keyword) + "%";
        String sql = SELECT_BASE + "WHERE (c.description LIKE ? OR c.building LIKE ? OR c.location LIKE ? "
                + "OR su.name LIKE ? OR CAST(c.complaint_id AS CHAR) = ?)" + ORDER_NEWEST_FIRST;
        return query(conn, sql, List.of(like, like, like, like, keyword.replaceAll("(?i)^CF-0*", "")));
    }

    /** Any argument may be null, meaning "do not filter on this". */
    public List<Complaint> filter(Connection conn, Category category, Priority priority, ComplaintStatus status)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (category != null) {
            sql.append(" AND c.category = ?");
            params.add(category.name());
        }
        if (priority != null) {
            sql.append(" AND c.priority = ?");
            params.add(priority.name());
        }
        if (status != null) {
            sql.append(" AND c.status = ?");
            params.add(status.name());
        }
        sql.append(ORDER_NEWEST_FIRST);
        return query(conn, sql.toString(), params);
    }

    /**
     * Compare-and-set status change: succeeds only if the row still has {@code expected} status.
     * Also sets or clears resolved_at.
     *
     * @return number of rows changed (0 means somebody else changed the complaint first)
     */
    public int updateStatus(Connection conn, int complaintId, ComplaintStatus expected, ComplaintStatus newStatus)
            throws SQLException {
        String sql = "UPDATE complaints SET status = ?, "
                + "resolved_at = CASE WHEN ? = 'RESOLVED' THEN CURRENT_TIMESTAMP ELSE NULL END "
                + "WHERE complaint_id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, newStatus.name());
            ps.setInt(3, complaintId);
            ps.setString(4, expected.name());
            return ps.executeUpdate();
        }
    }

    public int updatePriority(Connection conn, int complaintId, Priority priority) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE complaints SET priority = ? WHERE complaint_id = ?")) {
            ps.setString(1, priority.name());
            ps.setInt(2, complaintId);
            return ps.executeUpdate();
        }
    }

    public int setAssignedTechnician(Connection conn, int complaintId, Integer technicianId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE complaints SET assigned_technician_id = ? WHERE complaint_id = ?")) {
            if (technicianId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, technicianId);
            }
            ps.setInt(2, complaintId);
            return ps.executeUpdate();
        }
    }

    /** Edits details, but only for the owner's complaint and only while it is still SUBMITTED. */
    public int updateDetails(Connection conn, Complaint c) throws SQLException {
        String sql = "UPDATE complaints SET category = ?, description = ?, building = ?, location = ?, priority = ? "
                + "WHERE complaint_id = ? AND submitted_by = ? AND status = 'SUBMITTED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCategory().name());
            ps.setString(2, c.getDescription());
            ps.setString(3, c.getBuilding());
            ps.setString(4, c.getLocation());
            ps.setString(5, c.getPriority().name());
            ps.setInt(6, c.getComplaintId());
            ps.setInt(7, c.getSubmittedBy());
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ helpers

    private List<Complaint> query(Connection conn, String sql, List<?> params) throws SQLException {
        List<Complaint> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer n) {
                    ps.setInt(i + 1, n);
                } else {
                    ps.setString(i + 1, String.valueOf(p));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(map(rs));
                }
            }
        }
        return result;
    }

    private static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Complaint map(ResultSet rs) throws SQLException {
        Complaint c = new Complaint();
        c.setComplaintId(rs.getInt("complaint_id"));
        c.setSubmittedBy(rs.getInt("submitted_by"));
        c.setSubmittedByName(rs.getString("submitter_name"));
        c.setCategory(Category.valueOf(rs.getString("category")));
        c.setDescription(rs.getString("description"));
        c.setBuilding(rs.getString("building"));
        c.setLocation(rs.getString("location"));
        c.setPriority(Priority.valueOf(rs.getString("priority")));
        c.setStatus(ComplaintStatus.valueOf(rs.getString("status")));
        int techId = rs.getInt("assigned_technician_id");
        c.setAssignedTechnicianId(rs.wasNull() ? null : techId);
        c.setAssignedTechnicianName(rs.getString("technician_name"));
        c.setCreatedAt(toLocal(rs.getTimestamp("created_at")));
        c.setUpdatedAt(toLocal(rs.getTimestamp("updated_at")));
        c.setResolvedAt(toLocal(rs.getTimestamp("resolved_at")));
        return c;
    }

    private static java.time.LocalDateTime toLocal(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
