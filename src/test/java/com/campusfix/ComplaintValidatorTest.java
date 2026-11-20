package com.campusfix;

import com.campusfix.exception.ValidationException;
import com.campusfix.model.Category;
import com.campusfix.model.Priority;
import com.campusfix.validation.ComplaintValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComplaintValidatorTest {

    private static final String DESC = "The projector in the room does not switch on.";

    @Test
    void validComplaintPasses() {
        assertDoesNotThrow(() -> ComplaintValidator.validateComplaint(
                Category.LAB_EQUIPMENT, DESC, "Main Block", "Room 204", Priority.HIGH));
    }

    @Test
    void emptyDescriptionIsRejected() {
        assertThrows(ValidationException.class, () -> ComplaintValidator.validateComplaint(
                Category.OTHER, "  ", "Main Block", "Room 204", Priority.LOW));
    }

    @Test
    void tooShortDescriptionIsRejected() {
        assertThrows(ValidationException.class, () -> ComplaintValidator.validateComplaint(
                Category.OTHER, "broken", "Main Block", "Room 204", Priority.LOW));
    }

    @Test
    void tooLongDescriptionIsRejected() {
        String longText = "x".repeat(ComplaintValidator.MAX_DESCRIPTION_LENGTH + 1);
        assertThrows(ValidationException.class, () -> ComplaintValidator.validateComplaint(
                Category.OTHER, longText, "Main Block", "Room 204", Priority.LOW));
    }

    @Test
    void descriptionAtMaximumLengthIsAccepted() {
        String maxText = "x".repeat(ComplaintValidator.MAX_DESCRIPTION_LENGTH);
        assertDoesNotThrow(() -> ComplaintValidator.validateComplaint(
                Category.OTHER, maxText, "Main Block", "Room 204", Priority.LOW));
    }

    @Test
    void buildingAndLocationAreRequired() {
        assertThrows(ValidationException.class, () -> ComplaintValidator.validateComplaint(
                Category.OTHER, DESC, "", "Room 204", Priority.LOW));
        assertThrows(ValidationException.class, () -> ComplaintValidator.validateComplaint(
                Category.OTHER, DESC, "Main Block", "   ", Priority.LOW));
    }

    @Test
    void categoryAndPriorityAreRequired() {
        assertThrows(ValidationException.class, () -> ComplaintValidator.validateComplaint(
                null, DESC, "Main Block", "Room 204", Priority.LOW));
        assertThrows(ValidationException.class, () -> ComplaintValidator.validateComplaint(
                Category.OTHER, DESC, "Main Block", "Room 204", null));
    }

    @Test
    void parsesCategoryAndPriorityIgnoringCase() {
        assertEquals(Category.PLUMBING, ComplaintValidator.parseCategory(" plumbing "));
        assertEquals(Priority.CRITICAL, ComplaintValidator.parsePriority("Critical"));
    }

    @Test
    void invalidCategoryOrPriorityTextIsRejected() {
        assertThrows(ValidationException.class, () -> ComplaintValidator.parseCategory("AIR_CONDITIONING"));
        assertThrows(ValidationException.class, () -> ComplaintValidator.parseCategory(null));
        assertThrows(ValidationException.class, () -> ComplaintValidator.parsePriority("URGENT"));
    }

    @Test
    void remarksRules() {
        assertThrows(ValidationException.class, () -> ComplaintValidator.requireRemarks("Reason", " "));
        assertDoesNotThrow(() -> ComplaintValidator.requireRemarks("Reason", "Duplicate of CF-000002"));
        assertDoesNotThrow(() -> ComplaintValidator.validateOptionalRemarks(null));
        assertThrows(ValidationException.class,
                () -> ComplaintValidator.validateOptionalRemarks("x".repeat(501)));
    }
}
