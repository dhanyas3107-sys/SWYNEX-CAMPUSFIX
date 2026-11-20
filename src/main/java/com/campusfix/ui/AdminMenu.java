package com.campusfix.ui;

import com.campusfix.AppContext;
import com.campusfix.model.*;
import com.campusfix.util.ConsoleUtil;
import com.campusfix.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

/** Menu for administrators. */
public class AdminMenu {

    private final AppContext ctx;

    public AdminMenu(AppContext ctx) {
        this.ctx = ctx;
    }

    public void run(User admin) {
        while (true) {
            ConsoleUtil.header(admin.getDashboardTitle() + "  -  " + admin.getName());
            System.out.println("1. View All Complaints");
            System.out.println("2. Search Complaints");
            System.out.println("3. Filter Complaints");
            System.out.println("4. Assign Technician");
            System.out.println("5. Manage Technicians");
            System.out.println("6. Update Complaint Priority");
            System.out.println("7. Generate Reports");
            System.out.println("8. View Complaint History");
            System.out.println("9. Review / Reject Complaint");
            System.out.println("10. Logout");
            switch (ConsoleUtil.readInt("Select an option: ", 1, 10)) {
                case 1 -> ConsoleUtil.runSafely(() -> viewAll(admin));
                case 2 -> ConsoleUtil.runSafely(() -> search(admin));
                case 3 -> ConsoleUtil.runSafely(() -> filter(admin));
                case 4 -> ConsoleUtil.runSafely(() -> assign(admin));
                case 5 -> manageTechnicians(admin);
                case 6 -> ConsoleUtil.runSafely(() -> changePriority(admin));
                case 7 -> ConsoleUtil.runSafely(() -> ComplaintPrinter.printReport(ctx.reportService.generate(admin)));
                case 8 -> ConsoleUtil.runSafely(() -> history(admin));
                case 9 -> ConsoleUtil.runSafely(() -> reviewOrReject(admin));
                default -> {
                    ConsoleUtil.info("Logged out.");
                    return;
                }
            }
        }
    }

    private void viewAll(User admin) {
        ComplaintPrinter.printTable(ctx.complaintService.getAllComplaints(admin));
        if (ConsoleUtil.confirm("Show full details of one complaint?")) {
            ComplaintPrinter.printDetails(ctx.complaintService.getComplaint(admin, askId("Complaint ID: ")));
        }
    }

    private void search(User admin) {
        ComplaintPrinter.printTable(ctx.complaintService.searchComplaints(admin,
                ConsoleUtil.readLine("Keyword (description, building, location, submitter or ID): ")));
    }

    private void filter(User admin) {
        ConsoleUtil.subHeader("Filter complaints");
        Category category = askOptional("Category", List.of(Category.values()));
        Priority priority = askOptional("Priority", List.of(Priority.values()));
        List<String> statusChoices = new ArrayList<>(List.of("ANY", "PENDING (not started yet)"));
        for (ComplaintStatus s : ComplaintStatus.values()) {
            statusChoices.add(s.name());
        }
        String picked = ConsoleUtil.choose("Status:", statusChoices, s -> s);
        if (picked.startsWith("PENDING")) {
            ComplaintPrinter.printTable(ctx.complaintService.getPendingComplaints(admin).stream()
                    .filter(c -> category == null || c.getCategory() == category)
                    .filter(c -> priority == null || c.getPriority() == priority).toList());
            return;
        }
        ComplaintStatus status = picked.equals("ANY") ? null : ComplaintStatus.valueOf(picked);
        ComplaintPrinter.printTable(ctx.complaintService.filterComplaints(admin, category, priority, status));
    }

    private void assign(User admin) {
        int complaintId = askId("Complaint ID to assign: ");
        Complaint complaint = ctx.complaintService.getComplaint(admin, complaintId);
        ComplaintPrinter.printDetails(complaint);
        List<TechnicianRecommendation> recommendations = ctx.technicianService.recommendFor(admin, complaintId);
        System.out.println();
        ComplaintPrinter.printRecommendations(recommendations);
        System.out.println("\nAll technicians:");
        ComplaintPrinter.printWorkloads(ctx.technicianService.getWorkloads(admin));
        String input = ConsoleUtil.readLine("\nTechnician ID to assign (Enter = use the recommended one, 0 = cancel): ");
        int technicianId;
        if (input.isEmpty()) {
            if (recommendations.isEmpty()) {
                ConsoleUtil.error("There is no recommended technician. Enter an ID manually.");
                return;
            }
            technicianId = recommendations.get(0).technician().getTechnicianId();
        } else {
            try {
                technicianId = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                ConsoleUtil.error("Technician ID must be a number.");
                return;
            }
            if (technicianId == 0) {
                ConsoleUtil.info("Nothing was changed.");
                return;
            }
        }
        String remarks = ConsoleUtil.readLine("Remarks (optional): ");
        Complaint updated = ctx.complaintService.assignTechnician(admin, complaintId, technicianId, remarks);
        ConsoleUtil.success("Complaint " + IdGenerator.formatComplaintId(complaintId) + " is now "
                + updated.getStatus() + " and assigned to " + updated.getAssignedTechnicianName() + ".");
    }

    private void manageTechnicians(User admin) {
        while (true) {
            ConsoleUtil.subHeader("Manage Technicians");
            System.out.println("1. View technicians, availability and workload");
            System.out.println("2. Add technician");
            System.out.println("3. Change availability");
            System.out.println("4. Enable / disable technician account");
            System.out.println("5. Back");
            int choice = ConsoleUtil.readInt("Select an option: ", 1, 5);
            if (choice == 5) {
                return;
            }
            ConsoleUtil.runSafely(() -> {
                switch (choice) {
                    case 1 -> ComplaintPrinter.printWorkloads(ctx.technicianService.getWorkloads(admin));
                    case 2 -> addTechnician(admin);
                    case 3 -> changeAvailability(admin);
                    default -> toggleAccount(admin);
                }
            });
        }
    }

    private void addTechnician(User admin) {
        String name = ConsoleUtil.readLine("Name: ");
        String email = ConsoleUtil.readLine("Email: ");
        String phone = ConsoleUtil.readLine("Phone: ");
        Category specialization = ConsoleUtil.choose("Specialization:", List.of(Category.values()), Category::getLabel);
        String contact = ConsoleUtil.readLine("Contact information (e.g. extension or office): ");
        String password = ConsoleUtil.readPassword("Temporary password (min 8 chars, upper, lower, digit, symbol): ");
        Technician t = ctx.technicianService.createTechnician(admin, name, email, phone, password, specialization, contact);
        ConsoleUtil.success("Technician created with ID " + t.getTechnicianId() + ". Share the password securely.");
    }

    private void changeAvailability(User admin) {
        int id = ConsoleUtil.readInt("Technician ID: ", 1, Integer.MAX_VALUE);
        Availability availability = ConsoleUtil.chooseEnum("New availability:", Availability.values());
        ctx.technicianService.setAvailability(admin, id, availability);
        ConsoleUtil.success("Availability updated.");
    }

    private void toggleAccount(User admin) {
        int id = ConsoleUtil.readInt("Technician ID: ", 1, Integer.MAX_VALUE);
        boolean enable = ConsoleUtil.choose("Account:", List.of("Enable", "Disable"), s -> s).equals("Enable");
        ctx.technicianService.setAccountActive(admin, id, enable);
        ConsoleUtil.success("Account " + (enable ? "enabled." : "disabled."));
    }

    private void changePriority(User admin) {
        int id = askId("Complaint ID: ");
        ComplaintPrinter.printDetails(ctx.complaintService.getComplaint(admin, id));
        Priority priority = ConsoleUtil.chooseEnum("New priority:", Priority.values());
        String reason = ConsoleUtil.readLine("Justification (required): ");
        ctx.complaintService.changePriority(admin, id, priority, reason);
        ConsoleUtil.success("Priority updated and recorded in the history.");
    }

    private void history(User admin) {
        int id = askId("Complaint ID: ");
        ConsoleUtil.subHeader("History of " + IdGenerator.formatComplaintId(id));
        ComplaintPrinter.printHistory(ctx.complaintService.getHistory(admin, id));
    }

    private void reviewOrReject(User admin) {
        int id = askId("Complaint ID: ");
        ComplaintPrinter.printDetails(ctx.complaintService.getComplaint(admin, id));
        String action = ConsoleUtil.choose("Action:", List.of("Start review", "Reject", "Back"), s -> s);
        if (action.equals("Start review")) {
            ctx.complaintService.startReview(admin, id, ConsoleUtil.readLine("Remarks (optional): "));
            ConsoleUtil.success("Complaint is now UNDER_REVIEW.");
        } else if (action.equals("Reject")) {
            ctx.complaintService.rejectComplaint(admin, id, ConsoleUtil.readLine("Rejection reason (required): "));
            ConsoleUtil.success("Complaint rejected.");
        }
    }

    private static int askId(String prompt) {
        return IdGenerator.parseComplaintId(ConsoleUtil.readLine(prompt));
    }

    /** Offers "ANY" plus the values; returns null for ANY. */
    private static <T> T askOptional(String title, List<T> values) {
        List<String> labels = new ArrayList<>();
        labels.add("ANY");
        values.forEach(v -> labels.add(String.valueOf(v)));
        String picked = ConsoleUtil.choose(title + ":", labels, s -> s);
        for (T value : values) {
            if (String.valueOf(value).equals(picked)) {
                return value;
            }
        }
        return null;
    }
}
