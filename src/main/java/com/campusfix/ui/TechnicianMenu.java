package com.campusfix.ui;

import com.campusfix.AppContext;
import com.campusfix.model.*;
import com.campusfix.util.ConsoleUtil;
import com.campusfix.util.IdGenerator;

import java.util.List;

/** Menu for technicians. Services guarantee they can only touch their own assignments. */
public class TechnicianMenu {

    private final AppContext ctx;

    public TechnicianMenu(AppContext ctx) {
        this.ctx = ctx;
    }

    public void run(User user) {
        while (true) {
            ConsoleUtil.header(user.getDashboardTitle() + "  -  " + user.getName());
            System.out.println("1. View Assigned Complaints");
            System.out.println("2. Accept Assignment");
            System.out.println("3. Update Complaint Status");
            System.out.println("4. Add Progress Notes");
            System.out.println("5. View Completed Tasks");
            System.out.println("6. Logout");
            switch (ConsoleUtil.readInt("Select an option: ", 1, 6)) {
                case 1 -> ConsoleUtil.runSafely(() -> viewAssigned(user));
                case 2 -> ConsoleUtil.runSafely(() -> accept(user));
                case 3 -> ConsoleUtil.runSafely(() -> updateStatus(user));
                case 4 -> ConsoleUtil.runSafely(() -> addNote(user));
                case 5 -> ConsoleUtil.runSafely(() -> ComplaintPrinter.printTable(ctx.complaintService.getCompletedTasks(user)));
                default -> {
                    ConsoleUtil.info("Logged out.");
                    return;
                }
            }
        }
    }

    private void viewAssigned(User user) {
        List<Complaint> list = ctx.complaintService.getAssignedComplaints(user);
        ComplaintPrinter.printTable(list);
        if (!list.isEmpty() && ConsoleUtil.confirm("Show full details of one complaint?")) {
            ComplaintPrinter.printDetails(ctx.complaintService.getComplaint(user, askId("Complaint ID: ")));
        }
    }

    private void accept(User user) {
        int id = askId("Complaint ID to accept: ");
        ctx.complaintService.acceptAssignment(user, id);
        ConsoleUtil.success("Assignment accepted. Status is now IN_PROGRESS.");
    }

    private void updateStatus(User user) {
        int id = askId("Complaint ID: ");
        ComplaintStatus target = ConsoleUtil.choose("New status:",
                List.of(ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED), ComplaintStatus::name);
        String notes = target == ComplaintStatus.RESOLVED ? ConsoleUtil.readLine("Resolution notes (required): ") : "";
        Complaint c = ctx.complaintService.updateStatusAsTechnician(user, id, target, notes);
        ConsoleUtil.success("Complaint " + IdGenerator.formatComplaintId(c.getComplaintId()) + " is now " + c.getStatus() + ".");
    }

    private void addNote(User user) {
        int id = askId("Complaint ID: ");
        ctx.complaintService.addProgressNote(user, id, ConsoleUtil.readLine("Progress note: "));
        ConsoleUtil.success("Progress note saved to the complaint history.");
    }

    private static int askId(String prompt) {
        return IdGenerator.parseComplaintId(ConsoleUtil.readLine(prompt));
    }
}
