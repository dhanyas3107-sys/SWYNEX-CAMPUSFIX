package com.campusfix.service;

import com.campusfix.dao.ComplaintDAO;
import com.campusfix.dao.TechnicianDAO;
import com.campusfix.dao.UserDAO;
import com.campusfix.database.DatabaseConnection;
import com.campusfix.exception.ComplaintNotFoundException;
import com.campusfix.exception.DatabaseException;
import com.campusfix.exception.ValidationException;
import com.campusfix.model.*;
import com.campusfix.util.PasswordUtil;
import com.campusfix.validation.InputValidator;
import com.campusfix.validation.UserValidator;

import java.util.*;

/**
 * Technician accounts, workload and the rule-based assignment recommender.
 *
 * <h2>Assignment recommendation logic (transparent, rule based - NOT artificial intelligence)</h2>
 * <ol>
 *   <li><b>Filter out</b> technicians who cannot take work: disabled accounts, ON_LEAVE, and anyone who
 *       already holds {@link #MAX_ACTIVE_ASSIGNMENTS} active assignments.</li>
 *   <li><b>Sort the rest</b> by these keys, in this order:
 *     <ol>
 *       <li>Specialization matches the complaint category (matching first).</li>
 *       <li>Availability: AVAILABLE before BUSY.</li>
 *       <li>Fewest active assignments (spreads work evenly).</li>
 *       <li>Lowest technician id (a stable tie-breaker so results are repeatable).</li>
 *     </ol>
 *   </li>
 * </ol>
 * The first entry of the sorted list is the recommendation; every entry carries a plain-English reason.
 */
public class TechnicianService {

    /** A technician may not hold more than this many active assignments. */
    public static final int MAX_ACTIVE_ASSIGNMENTS = 8;

    private final TechnicianDAO technicianDAO;
    private final UserDAO userDAO;
    private final ComplaintDAO complaintDAO;

    public TechnicianService(TechnicianDAO technicianDAO, UserDAO userDAO, ComplaintDAO complaintDAO) {
        this.technicianDAO = technicianDAO;
        this.userDAO = userDAO;
        this.complaintDAO = complaintDAO;
    }

    // ------------------------------------------------------------ pure business rules (unit-tested)

    /**
     * Rule check used before every assignment.
     *
     * @throws ValidationException if the technician cannot take another complaint
     */
    public static void validateAssignable(Technician technician, int activeAssignments) {
        if (!technician.isActive()) {
            throw new ValidationException("Technician " + technician.getName() + "'s account is disabled.");
        }
        if (technician.getAvailability() == Availability.ON_LEAVE) {
            throw new ValidationException("Technician " + technician.getName() + " is on leave.");
        }
        if (activeAssignments >= MAX_ACTIVE_ASSIGNMENTS) {
            throw new ValidationException("Technician " + technician.getName() + " already has "
                    + activeAssignments + " active assignments (maximum " + MAX_ACTIVE_ASSIGNMENTS + ").");
        }
    }

    /** Ranks technicians for a complaint category. See the class comment for the exact rules. */
    public static List<TechnicianRecommendation> rank(Category category, List<Technician> technicians,
                                                      Map<Integer, Integer> activeCounts) {
        List<TechnicianRecommendation> candidates = new ArrayList<>();
        for (Technician t : technicians) {
            int active = activeCounts.getOrDefault(t.getTechnicianId(), 0);
            try {
                validateAssignable(t, active);
            } catch (ValidationException excluded) {
                continue;
            }
            boolean match = t.getSpecialization() == category;
            candidates.add(new TechnicianRecommendation(t, active, match, explain(t, active, match)));
        }
        candidates.sort(Comparator
                .comparing((TechnicianRecommendation r) -> !r.specializationMatch())
                .thenComparing(r -> r.technician().getAvailability() != Availability.AVAILABLE)
                .thenComparingInt(TechnicianRecommendation::activeAssignments)
                .thenComparingInt(r -> r.technician().getTechnicianId()));
        return candidates;
    }

    private static String explain(Technician t, int active, boolean match) {
        return (match ? "specialist in " + t.getSpecialization().getLabel() : "specialises in "
                + t.getSpecialization().getLabel() + " (not an exact match)")
                + ", " + t.getAvailability() + ", " + active + " active assignment(s)";
    }

    // ------------------------------------------------------------ operations

    /** Recommended technicians for a complaint, best first. */
    public List<TechnicianRecommendation> recommendFor(User actor, int complaintId) {
        AuthorizationService.require(actor, Permission.ASSIGN_TECHNICIAN);
        return DatabaseConnection.read(conn -> {
            Complaint complaint = complaintDAO.findById(conn, complaintId, false)
                    .orElseThrow(() -> new ComplaintNotFoundException(complaintId));
            return rank(complaint.getCategory(), technicianDAO.findAll(conn), technicianDAO.activeAssignmentCounts(conn));
        });
    }

    public List<TechnicianWorkload> getWorkloads(User actor) {
        AuthorizationService.require(actor, Permission.MANAGE_TECHNICIANS);
        return DatabaseConnection.read(conn -> {
            Map<Integer, Integer> active = technicianDAO.activeAssignmentCounts(conn);
            List<TechnicianWorkload> list = new ArrayList<>();
            for (Technician t : technicianDAO.findAll(conn)) {
                list.add(new TechnicianWorkload(t, active.getOrDefault(t.getTechnicianId(), 0),
                        technicianDAO.countCompleted(conn, t.getTechnicianId())));
            }
            return list;
        });
    }

    /** Creates the login account and the technician profile in one transaction. */
    public Technician createTechnician(User actor, String name, String email, String phone, String password,
                                       Category specialization, String contactInfo) {
        AuthorizationService.require(actor, Permission.MANAGE_TECHNICIANS);
        UserValidator.validateRegistration(name, email, phone, password);
        UserValidator.validateTechnicianContact(contactInfo);
        if (specialization == null) {
            throw new ValidationException("Specialization must be selected.");
        }
        String cleanEmail = email.trim().toLowerCase();
        Technician technician = new Technician(0, name.trim(), cleanEmail, PasswordUtil.hashPassword(password),
                InputValidator.normalizePhone(phone), null, 0, specialization, contactInfo.trim(), Availability.AVAILABLE);
        try {
            DatabaseConnection.transaction(conn -> {
                if (userDAO.existsByEmail(conn, cleanEmail)) {
                    throw new ValidationException(UserService.DUPLICATE_EMAIL);
                }
                userDAO.insert(conn, technician);
                technicianDAO.insert(conn, technician);
                return null;
            });
        } catch (DatabaseException e) {
            if (e.isDuplicateKey()) {
                throw new ValidationException(UserService.DUPLICATE_EMAIL);
            }
            throw e;
        }
        return technician;
    }

    public void setAvailability(User actor, int technicianId, Availability availability) {
        AuthorizationService.require(actor, Permission.MANAGE_TECHNICIANS);
        DatabaseConnection.transaction(conn -> {
            if (!technicianDAO.updateAvailability(conn, technicianId, availability)) {
                throw new ValidationException("Technician not found.");
            }
            return null;
        });
    }

    /** Enables or disables a technician's login. A technician with active work cannot be disabled. */
    public void setAccountActive(User actor, int technicianId, boolean active) {
        AuthorizationService.require(actor, Permission.MANAGE_TECHNICIANS);
        DatabaseConnection.transaction(conn -> {
            Technician t = technicianDAO.findById(conn, technicianId)
                    .orElseThrow(() -> new ValidationException("Technician not found."));
            if (!active && technicianDAO.countActiveAssignments(conn, technicianId) > 0) {
                throw new ValidationException("This technician still has active assignments. Reassign them first.");
            }
            userDAO.setActive(conn, t.getUserId(), active);
            return null;
        });
    }
}
