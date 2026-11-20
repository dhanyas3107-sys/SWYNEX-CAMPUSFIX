package com.campusfix.ui;

import com.campusfix.model.*;
import com.campusfix.util.ConsoleUtil;
import com.campusfix.util.IdGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Formats complaints, histories, technicians and reports for the console. */
final class ComplaintPrinter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private ComplaintPrinter() { }

    static void printTable(List<Complaint> complaints) {
        if (complaints.isEmpty()) {
            System.out.println("No complaints found.");
            return;
        }
        System.out.printf("%-10s %-14s %-9s %-12s %-18s %-20s%n",
                "ID", "CATEGORY", "PRIORITY", "STATUS", "LOCATION", "TECHNICIAN");
        System.out.println("-".repeat(86));
        for (Complaint c : complaints) {
            System.out.printf("%-10s %-14s %-9s %-12s %-18s %-20s%n",
                    IdGenerator.formatComplaintId(c.getComplaintId()), c.getCategory(), c.getPriority(),
                    c.getStatus(), cut(c.getBuilding() + "/" + c.getLocation(), 18),
                    c.getAssignedTechnicianName() == null ? "-" : cut(c.getAssignedTechnicianName(), 20));
        }
        System.out.println(complaints.size() + " complaint(s).");
    }

    static void printDetails(Complaint c) {
        System.out.println();
        System.out.println("Complaint        : " + IdGenerator.formatComplaintId(c.getComplaintId()));
        System.out.println("Submitted by     : " + c.getSubmittedByName());
        System.out.println("Category         : " + c.getCategory().getLabel());
        System.out.println("Priority         : " + c.getPriority());
        System.out.println("Status           : " + c.getStatus());
        System.out.println("Building         : " + c.getBuilding());
        System.out.println("Location         : " + c.getLocation());
        System.out.println("Description      : " + c.getDescription());
        System.out.println("Technician       : " + (c.getAssignedTechnicianName() == null ? "Not assigned yet" : c.getAssignedTechnicianName()));
        System.out.println("Created          : " + time(c.getCreatedAt()));
        System.out.println("Last updated     : " + time(c.getUpdatedAt()));
        if (c.getResolvedAt() != null) {
            System.out.println("Resolved         : " + time(c.getResolvedAt()));
        }
    }

    static void printHistory(List<ComplaintHistory> history) {
        if (history.isEmpty()) {
            System.out.println("No history recorded.");
            return;
        }
        for (ComplaintHistory h : history) {
            String change = h.getPreviousStatus() == null ? "(new) -> " + h.getNewStatus()
                    : h.getPreviousStatus() == h.getNewStatus() ? h.getNewStatus() + " (note)"
                    : h.getPreviousStatus() + " -> " + h.getNewStatus();
            System.out.printf("%s | %s | by %s%n", time(h.getChangedAt()), change, h.getUpdatedByName());
            if (h.getRemarks() != null) {
                System.out.println("      " + h.getRemarks());
            }
        }
    }

    static void printWorkloads(List<TechnicianWorkload> workloads) {
        if (workloads.isEmpty()) {
            System.out.println("No technicians registered.");
            return;
        }
        System.out.printf("%-4s %-20s %-15s %-10s %-8s %-7s %-9s%n",
                "ID", "NAME", "SPECIALIZATION", "STATUS", "ACTIVE", "DONE", "ACCOUNT");
        System.out.println("-".repeat(82));
        for (TechnicianWorkload w : workloads) {
            Technician t = w.technician();
            System.out.printf("%-4d %-20s %-15s %-10s %-8d %-7d %-9s%n", t.getTechnicianId(), cut(t.getName(), 20),
                    t.getSpecialization(), t.getAvailability(), w.activeAssignments(), w.completedTasks(),
                    t.isActive() ? "enabled" : "disabled");
        }
    }

    static void printRecommendations(List<TechnicianRecommendation> recs) {
        if (recs.isEmpty()) {
            System.out.println("No technician is currently able to accept this complaint.");
            return;
        }
        System.out.println("Recommended technicians (best first):");
        for (int i = 0; i < recs.size(); i++) {
            TechnicianRecommendation r = recs.get(i);
            System.out.printf("  %s ID %-3d %-20s %s%n", i == 0 ? "*" : " ", r.technician().getTechnicianId(),
                    r.technician().getName(), r.reason());
        }
    }

    static void printReport(SystemReport r) {
        ConsoleUtil.header("SYSTEM REPORT");
        System.out.println("Total complaints        : " + r.total());
        System.out.println("Pending (not started)   : " + r.pending() + "   [submitted, under review, assigned]");
        System.out.println("In progress             : " + r.inProgress());
        System.out.println("Resolved                : " + r.resolved());
        printCounts("Complaints by status", r.byStatus());
        printCounts("Complaints by category", r.byCategory());
        printCounts("Complaints by priority", r.byPriority());
        System.out.println();
        System.out.println("Technician workload and completed tasks");
        printWorkloads(r.technicianWorkloads());
        System.out.println();
        System.out.println("Resolution statistics");
        ResolutionStats s = r.resolutionStats();
        if (s.resolvedCount() == 0) {
            System.out.println("  No complaints have been resolved yet.");
        } else {
            System.out.printf("  Resolved complaints : %d%n  Average time       : %s%n  Fastest            : %s%n  Slowest            : %s%n",
                    s.resolvedCount(), duration((long) s.averageMinutes()), duration(s.minMinutes()), duration(s.maxMinutes()));
        }
    }

    private static void printCounts(String title, Map<String, Integer> counts) {
        System.out.println();
        System.out.println(title);
        if (counts.isEmpty()) {
            System.out.println("  (none)");
        }
        counts.forEach((k, v) -> System.out.printf("  %-14s %3d  %s%n", k, v, "#".repeat(Math.min(v, 40))));
    }

    private static String duration(long minutes) {
        return (minutes / 60) + " h " + (minutes % 60) + " min";
    }

    static String time(LocalDateTime t) {
        return t == null ? "-" : TIME.format(t);
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "~";
    }
}
