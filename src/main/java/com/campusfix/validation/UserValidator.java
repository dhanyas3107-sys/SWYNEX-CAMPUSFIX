package com.campusfix.validation;

import com.campusfix.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;

/** Validates account details. All problems are reported together in one message. */
public final class UserValidator {

    private UserValidator() { }

    public static void validateRegistration(String name, String email, String phone, String password) {
        List<String> errors = new ArrayList<>();
        if (InputValidator.isBlank(name)) {
            errors.add("Name must not be empty.");
        } else if (!InputValidator.isValidName(name)) {
            errors.add("Name may contain only letters, spaces, dots, apostrophes and hyphens (max "
                    + InputValidator.MAX_NAME_LENGTH + " characters).");
        }
        if (!InputValidator.isValidEmail(email)) {
            errors.add("Email address is not valid.");
        }
        if (!InputValidator.isValidPhone(phone)) {
            errors.add("Phone number must contain 10 to 15 digits (an optional leading + is allowed).");
        }
        String passwordProblem = InputValidator.passwordProblem(password);
        if (passwordProblem != null) {
            errors.add(passwordProblem);
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(String.join(" ", errors));
        }
    }

    /** Extra fields needed when an administrator creates a technician. */
    public static void validateTechnicianContact(String contactInfo) {
        InputValidator.requireText("Contact information", contactInfo, 150);
    }
}
