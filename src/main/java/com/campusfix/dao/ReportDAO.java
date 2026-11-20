package com.campusfix.dao;

import com.campusfix.model.ResolutionStats;

import java.sql.*;
import java.util.*;

/** Aggregate queries (COUNT, GROUP BY, ORDER BY, AVG ...) used by the report screen. */
public class ReportDAO {

    /** @return status name -> number of complaints */
    public Map<String, Integer> countByStatus(Connection conn) throws SQLException {
        return countMap(conn, "SELECT status AS label, COUNT(*) AS total FROM complaints "
                + "GROUP BY status ORDER BY total DESC, label");
    }

    public Map<String, Integer> countByCategory(Connection conn) throws SQLException {
        return countMap(conn, "SELECT category AS label, COUNT(*) AS total FROM complaints "
                + "GROUP BY category ORDER BY total DESC, label");
    }

    public Map<String, Integer> countByPriority(Connection conn) throws SQLException {
        return countMap(conn, "SELECT priority AS label, COUNT(*) AS total FROM complaints "
                + "GROUP BY priority ORDER BY FIELD(priority, 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW')");
    }

    /** Raw rows: {technicianId, activeCount, completedCount}, ordered by current workload. */
    public List<int[]> technicianWorkloadRows(Connection conn) throws SQLException {
        String sql = "SELECT t.technician_id, "
                + "COUNT(CASE WHEN a.is_active = 1 THEN 1 END) AS active_count, "
                + "COUNT(CASE WHEN a.completed_at IS NOT NULL THEN 1 END) AS completed_count "
                + "FROM technicians t JOIN users u ON u.user_id = t.user_id "
                + "LEFT JOIN complaint_assignments a ON a.technician_id = t.technician_id "
                + "GROUP BY t.technician_id, u.name ORDER BY active_count DESC, completed_count DESC, u.name";
        List<int[]> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new int[]{rs.getInt("technician_id"), rs.getInt("active_count"), rs.getInt("completed_count")});
            }
        }
        return rows;
    }

    public ResolutionStats resolutionStats(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) AS resolved_count, "
                + "AVG(TIMESTAMPDIFF(MINUTE, created_at, resolved_at)) AS avg_minutes, "
                + "MIN(TIMESTAMPDIFF(MINUTE, created_at, resolved_at)) AS min_minutes, "
                + "MAX(TIMESTAMPDIFF(MINUTE, created_at, resolved_at)) AS max_minutes "
                + "FROM complaints WHERE status = 'RESOLVED' AND resolved_at IS NOT NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            int count = rs.getInt("resolved_count");
            if (count == 0) {
                return ResolutionStats.none();
            }
            return new ResolutionStats(count, rs.getDouble("avg_minutes"),
                    rs.getLong("min_minutes"), rs.getLong("max_minutes"));
        }
    }

    private Map<String, Integer> countMap(Connection conn, String sql) throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("label"), rs.getInt("total"));
            }
        }
        return map;
    }
}
