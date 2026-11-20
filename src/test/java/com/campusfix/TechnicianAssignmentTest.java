package com.campusfix;

import com.campusfix.exception.ValidationException;
import com.campusfix.model.*;
import com.campusfix.service.TechnicianService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TechnicianAssignmentTest {

    private static Technician tech(int id, Category spec, Availability availability) {
        return new Technician(100 + id, "Tech" + id, "t" + id + "@c.edu", "", "9876543210", null,
                id, spec, "ext " + id, availability);
    }

    @Test
    void technicianOnLeaveCannotBeAssigned() {
        assertThrows(ValidationException.class,
                () -> TechnicianService.validateAssignable(tech(1, Category.ELECTRICAL, Availability.ON_LEAVE), 0));
    }

    @Test
    void disabledTechnicianCannotBeAssigned() {
        Technician t = tech(1, Category.ELECTRICAL, Availability.AVAILABLE);
        t.setActive(false);
        assertThrows(ValidationException.class, () -> TechnicianService.validateAssignable(t, 0));
    }

    @Test
    void technicianAtCapacityCannotBeAssigned() {
        Technician t = tech(1, Category.ELECTRICAL, Availability.AVAILABLE);
        assertDoesNotThrow(() -> TechnicianService.validateAssignable(t, TechnicianService.MAX_ACTIVE_ASSIGNMENTS - 1));
        assertThrows(ValidationException.class,
                () -> TechnicianService.validateAssignable(t, TechnicianService.MAX_ACTIVE_ASSIGNMENTS));
    }

    @Test
    void busyTechnicianCanStillBeAssigned() {
        assertDoesNotThrow(() -> TechnicianService.validateAssignable(tech(1, Category.NETWORK, Availability.BUSY), 2));
    }

    @Test
    void specialistIsPreferredOverNonSpecialist() {
        Technician electrician = tech(1, Category.ELECTRICAL, Availability.AVAILABLE);
        Technician plumber = tech(2, Category.PLUMBING, Availability.AVAILABLE);
        var ranked = TechnicianService.rank(Category.PLUMBING, List.of(electrician, plumber), Map.of());
        assertEquals(2, ranked.get(0).technician().getTechnicianId());
        assertTrue(ranked.get(0).specializationMatch());
        assertFalse(ranked.get(1).specializationMatch());
    }

    @Test
    void lessBusySpecialistWins() {
        Technician a = tech(1, Category.NETWORK, Availability.AVAILABLE);
        Technician b = tech(2, Category.NETWORK, Availability.AVAILABLE);
        var ranked = TechnicianService.rank(Category.NETWORK, List.of(a, b), Map.of(1, 4, 2, 1));
        assertEquals(2, ranked.get(0).technician().getTechnicianId());
        assertEquals(1, ranked.get(0).activeAssignments());
    }

    @Test
    void availableBeatsBusyWhenSpecializationIsEqual() {
        Technician busy = tech(1, Category.NETWORK, Availability.BUSY);
        Technician free = tech(2, Category.NETWORK, Availability.AVAILABLE);
        var ranked = TechnicianService.rank(Category.NETWORK, List.of(busy, free), Map.of(2, 5));
        assertEquals(2, ranked.get(0).technician().getTechnicianId());
    }

    @Test
    void matchingSpecialistBeatsNonMatchingEvenIfMoreLoaded() {
        Technician specialist = tech(1, Category.FURNITURE, Availability.AVAILABLE);
        Technician other = tech(2, Category.OTHER, Availability.AVAILABLE);
        var ranked = TechnicianService.rank(Category.FURNITURE, List.of(other, specialist), Map.of(1, 6));
        assertEquals(1, ranked.get(0).technician().getTechnicianId());
    }

    @Test
    void ineligibleTechniciansAreExcluded() {
        Technician leave = tech(1, Category.NETWORK, Availability.ON_LEAVE);
        Technician full = tech(2, Category.NETWORK, Availability.AVAILABLE);
        Technician ok = tech(3, Category.OTHER, Availability.AVAILABLE);
        var ranked = TechnicianService.rank(Category.NETWORK, List.of(leave, full, ok),
                Map.of(2, TechnicianService.MAX_ACTIVE_ASSIGNMENTS));
        assertEquals(1, ranked.size());
        assertEquals(3, ranked.get(0).technician().getTechnicianId());
    }

    @Test
    void tieIsBrokenByLowestId() {
        var ranked = TechnicianService.rank(Category.OTHER,
                List.of(tech(5, Category.OTHER, Availability.AVAILABLE), tech(2, Category.OTHER, Availability.AVAILABLE)), Map.of());
        assertEquals(2, ranked.get(0).technician().getTechnicianId());
    }

    @Test
    void emptyListGivesNoRecommendation() {
        assertTrue(TechnicianService.rank(Category.OTHER, List.of(), Map.of()).isEmpty());
    }
}
