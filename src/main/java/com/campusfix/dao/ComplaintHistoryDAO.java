package com.campusfix.dao;

import com.campusfix.model.ComplaintHistory;
import com.campusfix.model.ComplaintStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** SQL for {@code complaint_history}. Rows are only ever inserted, never updated or deleted. */
public class ComplaintHistoryDAO {

    public void insert(Connection conn, ComplaintHistory history) throws SQLException {
        String sql = "INSERT INTO complaint_history (complaint_id, previous_status, new_status, updated_by, remarks) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, history.getComplaintId());
            if (history.getPreviousStatus() == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, history.getPreviousStatus().name());
            }
            ps.setString(3, history.getNewStatus().name());
            ps.setInt(4, history.getUpdatedBy());
            if (history.getRemarks() == null || history.getRemarks().isBlank()) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, history.getRemarks().trim());
            }
            ps.executeUpdate();
        }
    }

    /** @return the complaint's history, oldest entry first */
    public List<ComplaintHistory> findByComplaint(Connection conn, int complaintId) throws SQLException {
        String sql = "SELECT h.history_id, h.complaint_id, h.previous_status, h.new_status, h.updated_by, "
                + "u.name AS updated_by_name, h.changed_at, h.remarks "
                + "FROM complaint_history h JOIN users u ON u.user_id = h.updated_by "
                + "WHERE h.complaint_id = ? ORDER BY h.history_id ASC";
        List<ComplaintHistory> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, complaintId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String previous = rs.getString("previous_status");
                    list.add(new ComplaintHistory(rs.getInt("history_id"), rs.getInt("complaint_id"),
                            previous == null ? null : ComplaintStatus.valueOf(previous),
                            ComplaintStatus.valueOf(rs.getString("new_status")),
                            rs.getInt("updated_by"), rs.getString("updated_by_name"),
                            rs.getTimestamp("changed_at").toLocalDateTime(), rs.getString("remarks")));
                }
            }
        }
        return list;
    }
}
