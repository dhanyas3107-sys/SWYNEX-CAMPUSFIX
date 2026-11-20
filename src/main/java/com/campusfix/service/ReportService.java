package com.campusfix.service;

import com.campusfix.dao.ReportDAO;
import com.campusfix.dao.TechnicianDAO;
import com.campusfix.database.DatabaseConnection;
import com.campusfix.model.*;

import java.util.*;

/** Builds the administrator's {@link SystemReport} from SQL aggregate queries. */
public class ReportService {

    private final ReportDAO reportDAO;
    private final TechnicianDAO technicianDAO;

    public ReportService(ReportDAO reportDAO, TechnicianDAO technicianDAO) {
        this.reportDAO = reportDAO;
        this.technicianDAO = technicianDAO;
    }

    public SystemReport generate(User actor) {
        AuthorizationService.require(actor, Permission.GENERATE_REPORTS);
        return DatabaseConnection.read(conn -> {
            Map<String, Integer> byStatus = reportDAO.countByStatus(conn);
            List<TechnicianWorkload> workloads = new ArrayList<>();
            Map<Integer, Technician> technicians = new HashMap<>();
            technicianDAO.findAll(conn).forEach(t -> technicians.put(t.getTechnicianId(), t));
            for (int[] row : reportDAO.technicianWorkloadRows(conn)) {
                Technician t = technicians.get(row[0]);
                if (t != null) {
                    workloads.add(new TechnicianWorkload(t, row[1], row[2]));
                }
            }
            return new SystemReport(sum(byStatus, null), count(byStatus, ComplaintStatus.RESOLVED),
                    sum(byStatus, true), count(byStatus, ComplaintStatus.IN_PROGRESS), byStatus,
                    reportDAO.countByCategory(conn), reportDAO.countByPriority(conn),
                    workloads, reportDAO.resolutionStats(conn));
        });
    }

    private static int count(Map<String, Integer> byStatus, ComplaintStatus status) {
        return byStatus.getOrDefault(status.name(), 0);
    }

    /** pendingOnly == null: all complaints; true: only SUBMITTED, UNDER_REVIEW and ASSIGNED. */
    private static int sum(Map<String, Integer> byStatus, Boolean pendingOnly) {
        int total = 0;
        for (Map.Entry<String, Integer> e : byStatus.entrySet()) {
            if (pendingOnly == null || ComplaintStatus.valueOf(e.getKey()).isPending()) {
                total += e.getValue();
            }
        }
        return total;
    }
}
