package com.campusfix;

import com.campusfix.exception.ValidationException;
import com.campusfix.validation.UserValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserValidatorTest {

    @Test
    void validRegistrationPasses() {
        assertDoesNotThrow(() -> UserValidator.validateRegistration(
                "Asha Kumar", "asha@college.edu", "9876543210", "Str0ng!Pass"));
    }

    @Test
    void emptyNameIsRejected() {
        ValidationException e = assertThrows(ValidationException.class, () -> UserValidator.validateRegistration(
                " ", "asha@college.edu", "9876543210", "Str0ng!Pass"));
        assertTrue(e.getMessage().contains("Name"));
    }

    @Test
    void allProblemsAreReportedTogether() {
        ValidationException e = assertThrows(ValidationException.class, () -> UserValidator.validateRegistration(
                "", "bad-email", "123", "weak"));
        assertTrue(e.getMessage().contains("Name"));
        assertTrue(e.getMessage().contains("Email"));
        assertTrue(e.getMessage().contains("Phone"));
        assertTrue(e.getMessage().contains("Password"));
    }

    @Test
    void technicianContactIsRequired() {
        assertThrows(ValidationException.class, () -> UserValidator.validateTechnicianContact(""));
        assertDoesNotThrow(() -> UserValidator.validateTechnicianContact("Ext. 204"));
    }
}
