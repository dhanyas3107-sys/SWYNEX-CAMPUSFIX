package com.campusfix.ui;

import com.campusfix.AppContext;
import com.campusfix.model.*;
import com.campusfix.util.ConsoleUtil;
import com.campusfix.util.IdGenerator;

import java.util.List;

/** Menu shared by students and faculty (both report problems). */
public class SubmitterMenu {

    private final AppContext ctx;

    public SubmitterMenu(AppContext ctx) {
        this.ctx = ctx;
    }

    public void run(User user) {
        while (true) {
            ConsoleUtil.header(user.getDashboardTitle() + "  -  " + user.getName());
            System.out.println("1. Submit Complaint");
            System.out.println("2. View My Complaints");
            System.out.println("3. Track Complaint");
            System.out.println("4. View Complaint History");
            System.out.println("5. Cancel Complaint");
            System.out.println("6. Edit Complaint (only before review starts)");
            System.out.println("7. Logout");
            switch (ConsoleUtil.readInt("Select an option: ", 1, 7)) {
                case 1 -> ConsoleUtil.runSafely(() -> submit(user));
                case 2 -> ConsoleUtil.runSafely(() -> ComplaintPrinter.printTable(ctx.complaintService.getMyComplaints(user)));
                case 3 -> ConsoleUtil.runSafely(() -> track(user));
                case 4 -> ConsoleUtil.runSafely(() -> history(user));
                case 5 -> ConsoleUtil.runSafely(() -> cancel(user));
                case 6 -> ConsoleUtil.runSafely(() -> edit(user));
                default -> {
                    ConsoleUtil.info("Logged out.");
                    return;
                }
            }
        }
    }

    private void submit(User user) {
        ConsoleUtil.subHeader("Submit a new complaint");
        Category category = ConsoleUtil.choose("Category:", List.of(Category.values()), Category::getLabel);
        String description = ConsoleUtil.readLine("Describe the problem (min 10 characters): ");
        String building = ConsoleUtil.readLine("Building: ");
        String location = ConsoleUtil.readLine("Location (room / floor / area): ");
        Priority priority = ConsoleUtil.chooseEnum("Priority:", Priority.values());
        Complaint c = ctx.complaintService.submitComplaint(user, category, description, building, location, priority);
        ConsoleUtil.success("Complaint submitted. Your complaint ID is " + IdGenerator.formatComplaintId(c.getComplaintId()));
    }

    private void track(User user) {
        int id = IdGenerator.parseComplaintId(ConsoleUtil.readLine("Complaint ID: "));
        ComplaintPrinter.printDetails(ctx.complaintService.getComplaint(user, id));
    }

    private void history(User user) {
        int id = IdGenerator.parseComplaintId(ConsoleUtil.readLine("Complaint ID: "));
        ConsoleUtil.subHeader("History of " + IdGenerator.formatComplaintId(id));
        ComplaintPrinter.printHistory(ctx.complaintService.getHistory(user, id));
    }

    private void cancel(User user) {
        int id = IdGenerator.parseComplaintId(ConsoleUtil.readLine("Complaint ID to cancel: "));
        if (!ConsoleUtil.confirm("Cancel complaint " + IdGenerator.formatComplaintId(id) + "? This cannot be undone")) {
            ConsoleUtil.info("Nothing was changed.");
            return;
        }
        String reason = ConsoleUtil.readLine("Reason (optional): ");
        ctx.complaintService.cancelComplaint(user, id, reason);
        ConsoleUtil.success("Complaint cancelled.");
    }

    private void edit(User user) {
        int id = IdGenerator.parseComplaintId(ConsoleUtil.readLine("Complaint ID to edit: "));
        ComplaintPrinter.printDetails(ctx.complaintService.getComplaint(user, id));
        ConsoleUtil.info("\nEnter the new details.");
        Category category = ConsoleUtil.choose("Category:", List.of(Category.values()), Category::getLabel);
        String description = ConsoleUtil.readLine("Description: ");
        String building = ConsoleUtil.readLine("Building: ");
        String location = ConsoleUtil.readLine("Location: ");
        Priority priority = ConsoleUtil.chooseEnum("Priority:", Priority.values());
        ctx.complaintService.updateComplaintDetails(user, id, category, description, building, location, priority);
        ConsoleUtil.success("Complaint updated.");
    }
}
