package com.campusfix;

import com.campusfix.exception.ValidationException;
import com.campusfix.validation.InputValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputValidatorTest {

    @Test
    void acceptsValidEmails() {
        assertTrue(InputValidator.isValidEmail("student@college.edu"));
        assertTrue(InputValidator.isValidEmail("first.last+tag@mail.example.co.in"));
        assertTrue(InputValidator.isValidEmail("  padded@college.edu  "));
    }

    @Test
    void rejectsInvalidEmails() {
        assertFalse(InputValidator.isValidEmail(null));
        assertFalse(InputValidator.isValidEmail(""));
        assertFalse(InputValidator.isValidEmail("no-at-sign.com"));
        assertFalse(InputValidator.isValidEmail("@college.edu"));
        assertFalse(InputValidator.isValidEmail("user@"));
        assertFalse(InputValidator.isValidEmail("user@college"));
        assertFalse(InputValidator.isValidEmail("user@@college.edu"));
        assertFalse(InputValidator.isValidEmail("us er@college.edu"));
        assertFalse(InputValidator.isValidEmail("user@college..edu"));
    }

    @Test
    void acceptsValidPhoneNumbers() {
        assertTrue(InputValidator.isValidPhone("9876543210"));
        assertTrue(InputValidator.isValidPhone("+919876543210"));
        assertTrue(InputValidator.isValidPhone("98765 43210"));
        assertTrue(InputValidator.isValidPhone("(987) 654-3210"));
    }

    @Test
    void rejectsInvalidPhoneNumbers() {
        assertFalse(InputValidator.isValidPhone(null));
        assertFalse(InputValidator.isValidPhone(""));
        assertFalse(InputValidator.isValidPhone("12345"));
        assertFalse(InputValidator.isValidPhone("98765abcde"));
        assertFalse(InputValidator.isValidPhone("1234567890123456"));
    }

    @Test
    void phoneIsNormalisedBeforeStorage() {
        assertEquals("+919876543210", InputValidator.normalizePhone("+91 98765-43210"));
    }

    @Test
    void passwordRulesAreEnforced() {
        assertTrue(InputValidator.isStrongPassword("Str0ng!Pass"));
        assertFalse(InputValidator.isStrongPassword(null));
        assertFalse(InputValidator.isStrongPassword("Sh0rt!"));
        assertFalse(InputValidator.isStrongPassword("alllowercase1!"));
        assertFalse(InputValidator.isStrongPassword("ALLUPPERCASE1!"));
        assertFalse(InputValidator.isStrongPassword("NoDigitsHere!"));
        assertFalse(InputValidator.isStrongPassword("NoSymbol1234"));
        assertFalse(InputValidator.isStrongPassword("Aa1!" + "x".repeat(200)));
    }

    @Test
    void passwordProblemExplainsWhatIsWrong() {
        assertNull(InputValidator.passwordProblem("Str0ng!Pass"));
        assertTrue(InputValidator.passwordProblem("abc").contains("at least 8"));
    }

    @Test
    void nameRules() {
        assertTrue(InputValidator.isValidName("Anita D'Souza-Rao"));
        assertFalse(InputValidator.isValidName(""));
        assertFalse(InputValidator.isValidName("   "));
        assertFalse(InputValidator.isValidName("R2D2"));
        assertFalse(InputValidator.isValidName("a".repeat(101)));
    }

    @Test
    void requireTextChecksEmptyAndLength() {
        assertDoesNotThrow(() -> InputValidator.requireText("Field", "ok", 5));
        assertThrows(ValidationException.class, () -> InputValidator.requireText("Field", "  ", 5));
        assertThrows(ValidationException.class, () -> InputValidator.requireText("Field", "toolong", 5));
    }
}
