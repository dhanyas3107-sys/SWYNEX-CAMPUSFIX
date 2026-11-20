package com.campusfix;

import com.campusfix.database.DatabaseConnection;
import com.campusfix.exception.InvalidStatusTransitionException;
import com.campusfix.exception.UnauthorizedOperationException;
import com.campusfix.exception.ValidationException;
import com.campusfix.model.*;
import com.campusfix.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test that talks to a REAL MySQL database.
 *
 * <p>It is NOT run by {@code mvn clean test} (its name ends in IT, and it is disabled unless
 * CAMPUSFIX_IT=true). Run it against a throw-away database only - see README, "Integration test".
 * <pre>
 *   export CAMPUSFIX_IT=true
 *   export CAMPUSFIX_DB_URL=jdbc:mysql://localhost:3306/campusfix_test
 *   export CAMPUSFIX_DB_USER=...   CAMPUSFIX_DB_PASSWORD=...
 *   mvn test -Dtest=CampusFixWorkflowIT
 * </pre>
 * Every account uses a unique random email, so the test can be re-run on the same database.
 */
@EnabledIfEnvironmentVariable(named = "CAMPUSFIX_IT", matches = "true")
class CampusFixWorkflowIT {

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@campusfix.test";
    }

    private static String randomPassword() {
        return "Aa1!" + UUID.randomUUID();
    }

    @Test
    void completeComplaintLifecycle() {
        AppContext ctx = new AppContext();

        // --- accounts -------------------------------------------------------------------
        String studentPassword = randomPassword();
        String studentEmail = uniqueEmail("student");
        User student = ctx.userService.registerStudent("Test Student", studentEmail, "9876543210", studentPassword);
        assertTrue(student.getUserId() > 0);
        assertThrows(ValidationException.class,
                () -> ctx.userService.registerStudent("Again", studentEmail, "9876543210", studentPassword));
        assertEquals(student.getUserId(), ctx.authenticationService.login(studentEmail, studentPassword).getUserId());
        assertThrows(com.campusfix.exception.AuthenticationException.class,
                () -> ctx.authenticationService.login(studentEmail, "wrong-Password1!"));

        // the administrator is inserted directly because createInitialAdministrator() refuses to run twice
        Administrator admin = new Administrator(0, "Test Admin", uniqueEmail("admin"),
                PasswordUtil.hashPassword(randomPassword()), "9876543210", null);
        DatabaseConnection.transaction(c -> new com.campusfix.dao.UserDAO().insert(c, admin));

        Technician electrician = ctx.technicianService.createTechnician(admin, "Test Electrician", uniqueEmail("tech"),
                "9876543211", randomPassword(), Category.ELECTRICAL, "ext 1");
        Technician other = ctx.technicianService.createTechnician(admin, "Other Tech", uniqueEmail("tech"),
                "9876543212", randomPassword(), Category.PLUMBING, "ext 2");

        // --- submit ---------------------------------------------------------------------
        Complaint complaint = ctx.complaintService.submitComplaint(student, Category.ELECTRICAL,
                "Tube light flickers constantly in the lab.", "Main Block", "Lab 3", Priority.MEDIUM);
        int id = complaint.getComplaintId();
        assertEquals(ComplaintStatus.SUBMITTED, complaint.getStatus());

        // --- authorisation --------------------------------------------------------------
        assertThrows(UnauthorizedOperationException.class,
                () -> ctx.complaintService.assignTechnician(student, id, electrician.getTechnicianId(), null));
        assertThrows(UnauthorizedOperationException.class, () -> ctx.complaintService.getAllComplaints(student));

        // --- recommendation + assignment -------------------------------------------------
        List<TechnicianRecommendation> recs = ctx.technicianService.recommendFor(admin, id);
        assertFalse(recs.isEmpty());
        Complaint assigned = ctx.complaintService.assignTechnician(admin, id, electrician.getTechnicianId(), "Urgent");
        assertEquals(ComplaintStatus.ASSIGNED, assigned.getStatus());
        assertEquals(Integer.valueOf(electrician.getTechnicianId()), assigned.getAssignedTechnicianId());

        // --- invalid transitions are rejected -----------------------------------------------
        assertThrows(InvalidStatusTransitionException.class, () -> ctx.complaintService.cancelComplaint(student, id, "x"));
        assertThrows(UnauthorizedOperationException.class, () -> ctx.complaintService.acceptAssignment(other, id));
        assertThrows(InvalidStatusTransitionException.class,
                () -> ctx.complaintService.resolveComplaint(electrician, id, "too early"));

        // --- technician workflow ---------------------------------------------------------
        assertEquals(ComplaintStatus.IN_PROGRESS, ctx.complaintService.acceptAssignment(electrician, id).getStatus());
        ctx.complaintService.addProgressNote(electrician, id, "Replaced the starter, waiting for new tube.");
        ctx.complaintService.changePriority(admin, id, Priority.HIGH, "Exam week - lab is in use");
        Complaint resolved = ctx.complaintService.resolveComplaint(electrician, id, "Tube replaced");
        assertEquals(ComplaintStatus.RESOLVED, resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
        assertThrows(InvalidStatusTransitionException.class,
                () -> ctx.complaintService.cancelComplaint(student, id, "too late"));

        // --- history ---------------------------------------------------------------------
        List<ComplaintHistory> history = ctx.complaintService.getHistory(student, id);
        // submitted, under review, assigned, in progress, note, priority change, resolved
        assertEquals(7, history.size());
        assertNull(history.get(0).getPreviousStatus());
        assertEquals(ComplaintStatus.RESOLVED, history.get(history.size() - 1).getNewStatus());

        // --- reports ---------------------------------------------------------------------
        SystemReport report = ctx.reportService.generate(admin);
        assertTrue(report.total() >= 1);
        assertTrue(report.resolved() >= 1);
        assertTrue(report.technicianWorkloads().stream()
                .anyMatch(w -> w.technician().getTechnicianId() == electrician.getTechnicianId() && w.completedTasks() == 1));
    }
}
