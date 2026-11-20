package com.campusfix;

import com.campusfix.exception.UnauthorizedOperationException;
import com.campusfix.model.*;
import com.campusfix.service.AuthorizationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationTest {

    private final User student = new Student(1, "Stu", "s@c.edu", "h", "9876543210", null);
    private final User faculty = new Faculty(2, "Fac", "f@c.edu", "h", "9876543210", null);
    private final User admin = new Administrator(3, "Adm", "a@c.edu", "h", "9876543210", null);
    private final User tech = new Technician(4, "Tec", "t@c.edu", "h", "9876543210", null, 1,
            Category.ELECTRICAL, "ext 1", Availability.AVAILABLE);

    @Test
    void eachSubclassReportsItsRole() { // polymorphism + overriding
        assertEquals(Role.STUDENT, student.getRole());
        assertEquals(Role.FACULTY, faculty.getRole());
        assertEquals(Role.ADMINISTRATOR, admin.getRole());
        assertEquals(Role.TECHNICIAN, tech.getRole());
    }

    @Test
    void studentsAndFacultyMaySubmitButNotAdminister() {
        for (User u : new User[]{student, faculty}) {
            assertDoesNotThrow(() -> AuthorizationService.require(u, Permission.SUBMIT_COMPLAINT));
            assertDoesNotThrow(() -> AuthorizationService.require(u, Permission.CANCEL_OWN_COMPLAINT));
            assertThrows(UnauthorizedOperationException.class, () -> AuthorizationService.require(u, Permission.ASSIGN_TECHNICIAN));
            assertThrows(UnauthorizedOperationException.class, () -> AuthorizationService.require(u, Permission.VIEW_ALL_COMPLAINTS));
            assertThrows(UnauthorizedOperationException.class, () -> AuthorizationService.require(u, Permission.GENERATE_REPORTS));
            assertThrows(UnauthorizedOperationException.class, () -> AuthorizationService.require(u, Permission.UPDATE_ASSIGNED_COMPLAINTS));
        }
    }

    @Test
    void administratorCanManageButNotSubmitOrWorkAsTechnician() {
        assertDoesNotThrow(() -> AuthorizationService.require(admin, Permission.ASSIGN_TECHNICIAN));
        assertDoesNotThrow(() -> AuthorizationService.require(admin, Permission.MANAGE_TECHNICIANS));
        assertDoesNotThrow(() -> AuthorizationService.require(admin, Permission.GENERATE_REPORTS));
        assertDoesNotThrow(() -> AuthorizationService.require(admin, Permission.CHANGE_PRIORITY));
        assertThrows(UnauthorizedOperationException.class, () -> AuthorizationService.require(admin, Permission.SUBMIT_COMPLAINT));
        assertThrows(UnauthorizedOperationException.class, () -> AuthorizationService.require(admin, Permission.UPDATE_ASSIGNED_COMPLAINTS));
    }

    @Test
    void technicianCanWorkOnAssignmentsButNotAdminister() {
        assertDoesNotThrow(() -> AuthorizationService.require(tech, Permission.VIEW_ASSIGNED_COMPLAINTS));
        assertDoesNotThrow(() -> AuthorizationService.require(tech, Permission.UPDATE_ASSIGNED_COMPLAINTS));
        assertThrows(UnauthorizedOperationException.class, () -> AuthorizationService.require(tech, Permission.ASSIGN_TECHNICIAN));
        assertThrows(UnauthorizedOperationException.class, () -> AuthorizationService.require(tech, Permission.CHANGE_PRIORITY));
        assertThrows(UnauthorizedOperationException.class, () -> AuthorizationService.require(tech, Permission.SUBMIT_COMPLAINT));
    }

    @Test
    void nobodyLoggedInIsRejected() {
        assertThrows(UnauthorizedOperationException.class, () -> AuthorizationService.require(null, Permission.SUBMIT_COMPLAINT));
        assertFalse(AuthorizationService.isAllowed(null, Permission.SUBMIT_COMPLAINT));
        assertTrue(AuthorizationService.isAllowed(student, Permission.SUBMIT_COMPLAINT));
    }

    @Test
    void toStringNeverLeaksPasswordHash() {
        User u = new Student(9, "Leaky", "l@c.edu", "SECRET-HASH-VALUE", "9876543210", null);
        assertFalse(u.toString().contains("SECRET-HASH-VALUE"));
    }

    @Test
    void dashboardTitlesAreOverridden() {
        assertNotEquals(student.getDashboardTitle(), admin.getDashboardTitle());
        assertTrue(tech.getDashboardTitle().contains("Electrical"));
    }
}
