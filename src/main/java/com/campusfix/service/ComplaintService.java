package com.campusfix.service;

import com.campusfix.dao.ComplaintDAO;
import com.campusfix.dao.ComplaintHistoryDAO;
import com.campusfix.dao.TechnicianDAO;
import com.campusfix.database.DatabaseConnection;
import com.campusfix.exception.*;
import com.campusfix.model.*;
import com.campusfix.validation.ComplaintValidator;
import com.campusfix.validation.InputValidator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * All complaint business rules: who may do what, which status changes are legal, and keeping the
 * history trail. Every operation that changes data runs inside ONE database transaction, so a status
 * change and its history row are committed together or not at all.
 */
public class ComplaintService {

    private static final Set<ComplaintStatus> TECHNICIAN_ACTIVE = EnumSet.of(ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS);
    private static final Set<ComplaintStatus> PENDING = EnumSet.of(
            ComplaintStatus.SUBMITTED, ComplaintStatus.UNDER_REVIEW, ComplaintStatus.ASSIGNED);

    private final ComplaintDAO complaintDAO;
    private final ComplaintHistoryDAO historyDAO;
    private final TechnicianDAO technicianDAO;

    public ComplaintService(ComplaintDAO complaintDAO, ComplaintHistoryDAO historyDAO, TechnicianDAO technicianDAO) {
        this.complaintDAO = complaintDAO;
        this.historyDAO = historyDAO;
        this.technicianDAO = technicianDAO;
    }

    // =================================================================== student / faculty

    public Complaint submitComplaint(User actor, Category category, String description,
                                     String building, String location, Priority priority) {
        AuthorizationService.require(actor, Permission.SUBMIT_COMPLAINT);
        ComplaintValidator.validateComplaint(category, description, building, location, priority);

        Complaint complaint = new Complaint();
        complaint.setSubmittedBy(actor.getUserId());
        complaint.setCategory(category);
        complaint.setDescription(description.trim());
        complaint.setBuilding(building.trim());
        complaint.setLocation(location.trim());
        complaint.setPriority(priority);
        complaint.setStatus(ComplaintStatus.SUBMITTED);

        return DatabaseConnection.transaction(conn -> {
            int id = complaintDAO.insert(conn, complaint);
            historyDAO.insert(conn, new ComplaintHistory(id, null, ComplaintStatus.SUBMITTED,
                    actor.getUserId(), "Complaint submitted"));
            return load(conn, id, false);
        });
    }

    public List<Complaint> getMyComplaints(User actor) {
        AuthorizationService.require(actor, Permission.VIEW_OWN_COMPLAINTS);
        return DatabaseConnection.read(conn -> complaintDAO.findBySubmitter(conn, actor.getUserId()));
    }

    /** Edits a complaint's details. Only the owner may do it, and only while it is still SUBMITTED. */
    public Complaint updateComplaintDetails(User actor, int complaintId, Category category, String description,
                                            String building, String location, Priority priority) {
        AuthorizationService.require(actor, Permission.EDIT_OWN_COMPLAINT);
        ComplaintValidator.validateComplaint(category, description, building, location, priority);
        return DatabaseConnection.transaction(conn -> {
            Complaint existing = load(conn, complaintId, true);
            requireOwner(actor, existing);
            if (!existing.isEditable()) {
                throw new ValidationException("Only complaints in SUBMITTED status can be edited. This one is "
                        + existing.getStatus() + ".");
            }
            existing.setCategory(category);
            existing.setDescription(description.trim());
            existing.setBuilding(building.trim());
            existing.setLocation(location.trim());
            existing.setPriority(priority);
            if (complaintDAO.updateDetails(conn, existing) == 0) {
                throw new ValidationException("The complaint changed while you were editing it. Please try again.");
            }
            note(conn, existing, actor, "Complaint details edited by the submitter");
            return load(conn, complaintId, false);
        });
    }

    /** Cancels a complaint. Allowed only for its owner and only before it has been assigned. */
    public Complaint cancelComplaint(User actor, int complaintId, String reason) {
        AuthorizationService.require(actor, Permission.CANCEL_OWN_COMPLAINT);
        ComplaintValidator.validateOptionalRemarks(reason);
        return DatabaseConnection.transaction(conn -> {
            Complaint complaint = load(conn, complaintId, true);
            requireOwner(actor, complaint);
            applyTransition(conn, complaint, ComplaintStatus.CANCELLED, actor,
                    InputValidator.isBlank(reason) ? "Cancelled by the submitter" : "Cancelled by the submitter: " + reason.trim());
            return load(conn, complaintId, false);
        });
    }

    // =================================================================== viewing (all roles)

    /** Returns one complaint if the actor is allowed to see it (owner, administrator, or assigned technician). */
    public Complaint getComplaint(User actor, int complaintId) {
        return DatabaseConnection.read(conn -> {
            Complaint complaint = load(conn, complaintId, false);
            requireCanView(actor, complaint);
            return complaint;
        });
    }

    /** Chronological history (oldest first) for a complaint the actor may see. */
    public List<ComplaintHistory> getHistory(User actor, int complaintId) {
        return DatabaseConnection.read(conn -> {
            requireCanView(actor, load(conn, complaintId, false));
            return historyDAO.findByComplaint(conn, complaintId);
        });
    }

    // =================================================================== administrator

    public List<Complaint> getAllComplaints(User actor) {
        AuthorizationService.require(actor, Permission.VIEW_ALL_COMPLAINTS);
        return DatabaseConnection.read(complaintDAO::findAll);
    }

    public List<Complaint> getPendingComplaints(User actor) {
        AuthorizationService.require(actor, Permission.VIEW_ALL_COMPLAINTS);
        return DatabaseConnection.read(conn -> complaintDAO.findByStatuses(conn, PENDING));
    }

    public List<Complaint> searchComplaints(User actor, String keyword) {
        AuthorizationService.require(actor, Permission.VIEW_ALL_COMPLAINTS);
        InputValidator.requireText("Search keyword", keyword, 100);
        return DatabaseConnection.read(conn -> complaintDAO.search(conn, keyword.trim()));
    }

    /** Any filter argument may be null ("do not filter on this"). */
    public List<Complaint> filterComplaints(User actor, Category category, Priority priority, ComplaintStatus status) {
        AuthorizationService.require(actor, Permission.VIEW_ALL_COMPLAINTS);
        return DatabaseConnection.read(conn -> complaintDAO.filter(conn, category, priority, status));
    }

    /** SUBMITTED -> UNDER_REVIEW. */
    public Complaint startReview(User actor, int complaintId, String remarks) {
        AuthorizationService.require(actor, Permission.REVIEW_COMPLAINT);
        ComplaintValidator.validateOptionalRemarks(remarks);
        return DatabaseConnection.transaction(conn -> {
            Complaint complaint = load(conn, complaintId, true);
            applyTransition(conn, complaint, ComplaintStatus.UNDER_REVIEW, actor,
                    InputValidator.isBlank(remarks) ? "Review started" : remarks.trim());
            return load(conn, complaintId, false);
        });
    }

    /** SUBMITTED or UNDER_REVIEW -> REJECTED. A reason is mandatory. */
    public Complaint rejectComplaint(User actor, int complaintId, String reason) {
        AuthorizationService.require(actor, Permission.REVIEW_COMPLAINT);
        ComplaintValidator.requireRemarks("Rejection reason", reason);
        return DatabaseConnection.transaction(conn -> {
            Complaint complaint = load(conn, complaintId, true);
            applyTransition(conn, complaint, ComplaintStatus.REJECTED, actor, "Rejected: " + reason.trim());
            return load(conn, complaintId, false);
        });
    }

    /** Changes priority of an open complaint. A written justification is mandatory and is stored in the history. */
    public Complaint changePriority(User actor, int complaintId, Priority newPriority, String justification) {
        AuthorizationService.require(actor, Permission.CHANGE_PRIORITY);
        if (newPriority == null) {
            throw new ValidationException("Priority must be selected.");
        }
        ComplaintValidator.requireRemarks("Justification", justification);
        return DatabaseConnection.transaction(conn -> {
            Complaint complaint = load(conn, complaintId, true);
            if (complaint.getStatus().isTerminal()) {
                throw new ValidationException("The priority of a " + complaint.getStatus() + " complaint cannot be changed.");
            }
            if (complaint.getPriority() == newPriority) {
                throw new ValidationException("The complaint already has priority " + newPriority + ".");
            }
            Priority old = complaint.getPriority();
            complaintDAO.updatePriority(conn, complaintId, newPriority);
            note(conn, complaint, actor, "Priority changed from " + old + " to " + newPriority
                    + ". Reason: " + justification.trim());
            return load(conn, complaintId, false);
        });
    }

    /**
     * Assigns (or reassigns) a technician.
     * <ul>
     *   <li>SUBMITTED complaints pass through UNDER_REVIEW automatically (two history rows).</li>
     *   <li>ASSIGNED complaints keep their status and just get a new technician.</li>
     *   <li>IN_PROGRESS complaints go back to ASSIGNED so the new technician must accept them.</li>
     * </ul>
     * Everything - assignment rows, complaint update and history - happens in one transaction.
     */
    public Complaint assignTechnician(User actor, int complaintId, int technicianId, String remarks) {
        AuthorizationService.require(actor, Permission.ASSIGN_TECHNICIAN);
        ComplaintValidator.validateOptionalRemarks(remarks);
        return DatabaseConnection.transaction(conn -> {
            Complaint complaint = load(conn, complaintId, true);
            ComplaintStatus current = complaint.getStatus();
            if (current.isTerminal()) {
                throw new InvalidStatusTransitionException(current, ComplaintStatus.ASSIGNED);
            }
            Technician technician = technicianDAO.findById(conn, technicianId)
                    .orElseThrow(() -> new ValidationException("Technician not found."));
            if (Objects.equals(complaint.getAssignedTechnicianId(), technicianId)) {
                throw new ValidationException("This complaint is already assigned to " + technician.getName() + ".");
            }
            TechnicianService.validateAssignable(technician, technicianDAO.countActiveAssignments(conn, technicianId));

            boolean reassignment = complaint.getAssignedTechnicianId() != null;
            String previousName = complaint.getAssignedTechnicianName();
            if (current == ComplaintStatus.SUBMITTED) {
                applyTransition(conn, complaint, ComplaintStatus.UNDER_REVIEW, actor, "Moved to review for assignment");
            }
            technicianDAO.releaseActiveAssignment(conn, complaintId);
            technicianDAO.insertAssignment(conn, complaintId, technicianId, actor.getUserId());
            complaintDAO.setAssignedTechnician(conn, complaintId, technicianId);

            String text = (reassignment ? "Reassigned from " + previousName + " to " : "Assigned to ")
                    + technician.getName() + (InputValidator.isBlank(remarks) ? "" : ". " + remarks.trim());
            if (complaint.getStatus() == ComplaintStatus.ASSIGNED) {
                note(conn, complaint, actor, text);                      // reassignment, status unchanged
            } else {
                applyTransition(conn, complaint, ComplaintStatus.ASSIGNED, actor, text);
            }
            return load(conn, complaintId, false);
        });
    }

    // =================================================================== technician

    /** Active work (ASSIGNED and IN_PROGRESS) of the logged-in technician. */
    public List<Complaint> getAssignedComplaints(User actor) {
        Technician technician = requireTechnician(actor, Permission.VIEW_ASSIGNED_COMPLAINTS);
        return DatabaseConnection.read(conn ->
                complaintDAO.findByTechnician(conn, technician.getTechnicianId(), TECHNICIAN_ACTIVE));
    }

    public List<Complaint> getCompletedTasks(User actor) {
        Technician technician = requireTechnician(actor, Permission.VIEW_ASSIGNED_COMPLAINTS);
        return DatabaseConnection.read(conn ->
                complaintDAO.findByTechnician(conn, technician.getTechnicianId(), EnumSet.of(ComplaintStatus.RESOLVED)));
    }

    /** ASSIGNED -> IN_PROGRESS for a complaint assigned to this technician. */
    public Complaint acceptAssignment(User actor, int complaintId) {
        Technician technician = requireTechnician(actor, Permission.UPDATE_ASSIGNED_COMPLAINTS);
        return DatabaseConnection.transaction(conn -> {
            Complaint complaint = load(conn, complaintId, true);
            requireAssignedTo(technician, complaint);
            applyTransition(conn, complaint, ComplaintStatus.IN_PROGRESS, actor, "Assignment accepted; work started");
            technicianDAO.markAccepted(conn, complaintId, technician.getTechnicianId());
            return load(conn, complaintId, false);
        });
    }

    /** IN_PROGRESS -> RESOLVED. Resolution notes are mandatory. */
    public Complaint resolveComplaint(User actor, int complaintId, String resolutionNotes) {
        Technician technician = requireTechnician(actor, Permission.UPDATE_ASSIGNED_COMPLAINTS);
        ComplaintValidator.requireRemarks("Resolution notes", resolutionNotes);
        return DatabaseConnection.transaction(conn -> {
            Complaint complaint = load(conn, complaintId, true);
            requireAssignedTo(technician, complaint);
            applyTransition(conn, complaint, ComplaintStatus.RESOLVED, actor, "Resolved: " + resolutionNotes.trim());
            technicianDAO.markCompleted(conn, complaintId, technician.getTechnicianId());
            return load(conn, complaintId, false);
        });
    }

    /** Menu entry point for technicians: routes IN_PROGRESS / RESOLVED to the right workflow step. */
    public Complaint updateStatusAsTechnician(User actor, int complaintId, ComplaintStatus newStatus, String remarks) {
        return switch (newStatus) {
            case IN_PROGRESS -> acceptAssignment(actor, complaintId);
            case RESOLVED -> resolveComplaint(actor, complaintId, remarks);
            default -> throw new UnauthorizedOperationException(
                    "Technicians can only set a complaint to IN_PROGRESS or RESOLVED.");
        };
    }

    /** Adds a progress note (stored in the history) to an active complaint assigned to this technician. */
    public void addProgressNote(User actor, int complaintId, String noteText) {
        Technician technician = requireTechnician(actor, Permission.UPDATE_ASSIGNED_COMPLAINTS);
        ComplaintValidator.requireRemarks("Progress note", noteText);
        DatabaseConnection.transaction(conn -> {
            Complaint complaint = load(conn, complaintId, true);
            requireAssignedTo(technician, complaint);
            if (!TECHNICIAN_ACTIVE.contains(complaint.getStatus())) {
                throw new ValidationException("Notes can only be added while the complaint is ASSIGNED or IN_PROGRESS.");
            }
            note(conn, complaint, actor, "Progress note: " + noteText.trim());
            return null;
        });
    }

    // =================================================================== internal helpers

    private Complaint load(Connection conn, int complaintId, boolean lock) throws SQLException {
        return complaintDAO.findById(conn, complaintId, lock)
                .orElseThrow(() -> new ComplaintNotFoundException(complaintId));
    }

    /**
     * The ONE method that changes a complaint's status. It (1) checks the workflow rules, (2) updates
     * the row only if nobody changed it in the meantime, and (3) writes the history record. Callers
     * run it inside a transaction so (2) and (3) are committed together.
     */
    private void applyTransition(Connection conn, Complaint complaint, ComplaintStatus target,
                                 User actor, String remarks) throws SQLException {
        ComplaintStatus from = complaint.getStatus();
        from.requireTransitionTo(target);
        if (target == ComplaintStatus.RESOLVED && complaint.getAssignedTechnicianId() == null) {
            throw new InvalidStatusTransitionException(from, target, "A complaint can only be resolved by its assigned technician.");
        }
        if (complaintDAO.updateStatus(conn, complaint.getComplaintId(), from, target) == 0) {
            throw new ValidationException("This complaint was just changed by someone else. Please reload and try again.");
        }
        historyDAO.insert(conn, new ComplaintHistory(complaint.getComplaintId(), from, target, actor.getUserId(), remarks));
        complaint.setStatus(target);
    }

    /** Records a history row that does not change the status (previous = new). */
    private void note(Connection conn, Complaint complaint, User actor, String text) throws SQLException {
        historyDAO.insert(conn, new ComplaintHistory(complaint.getComplaintId(), complaint.getStatus(),
                complaint.getStatus(), actor.getUserId(), text));
    }

    private static void requireOwner(User actor, Complaint complaint) {
        if (!complaint.isOwnedBy(actor.getUserId())) {
            throw new UnauthorizedOperationException("You can only change your own complaints.");
        }
    }

    private static void requireCanView(User actor, Complaint complaint) {
        if (actor == null) {
            throw new UnauthorizedOperationException("You must be logged in to do this.");
        }
        boolean admin = actor.hasPermission(Permission.VIEW_ALL_COMPLAINTS);
        boolean owner = actor.hasPermission(Permission.VIEW_OWN_COMPLAINTS) && complaint.isOwnedBy(actor.getUserId());
        boolean assignedTechnician = actor instanceof Technician t
                && actor.hasPermission(Permission.VIEW_ASSIGNED_COMPLAINTS)
                && Objects.equals(complaint.getAssignedTechnicianId(), t.getTechnicianId());
        if (!(admin || owner || assignedTechnician)) {
            throw new UnauthorizedOperationException("You are not allowed to view this complaint.");
        }
    }

    private static Technician requireTechnician(User actor, Permission permission) {
        AuthorizationService.require(actor, permission);
        if (!(actor instanceof Technician technician)) {
            throw new UnauthorizedOperationException("Only technicians can do this.");
        }
        return technician;
    }

    private static void requireAssignedTo(Technician technician, Complaint complaint) {
        if (!Objects.equals(complaint.getAssignedTechnicianId(), technician.getTechnicianId())) {
            throw new UnauthorizedOperationException("This complaint is not assigned to you. You can only modify your own assignments.");
        }
    }
}
