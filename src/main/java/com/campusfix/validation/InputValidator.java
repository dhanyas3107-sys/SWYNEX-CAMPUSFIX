package com.campusfix.validation;

import com.campusfix.exception.ValidationException;

import java.util.regex.Pattern;

/** Small reusable checks. Higher-level validators combine these. */
public final class InputValidator {

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{10,15}$");
    private static final Pattern NAME = Pattern.compile("^[\\p{L}][\\p{L} .'-]*$");

    public static final int MAX_EMAIL_LENGTH = 150;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 128;

    private InputValidator() { }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.trim().length() <= MAX_EMAIL_LENGTH
                && EMAIL.matcher(email.trim()).matches();
    }

    /** Spaces, dashes and brackets are ignored; 10-15 digits with an optional leading '+'. */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(normalizePhone(phone)).matches();
    }

    public static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s\\-().]", "");
    }

    public static boolean isValidName(String name) {
        return !isBlank(name) && name.trim().length() <= MAX_NAME_LENGTH && NAME.matcher(name.trim()).matches();
    }

    /** At least 8 characters with an upper-case letter, a lower-case letter, a digit and a symbol. */
    public static boolean isStrongPassword(String password) {
        return passwordProblem(password) == null;
    }

    /** @return a description of what is wrong with the password, or null if it is acceptable */
    public static String passwordProblem(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.";
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            return "Password must be at most " + MAX_PASSWORD_LENGTH + " characters long.";
        }
        boolean upper = false, lower = false, digit = false, symbol = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) upper = true;
            else if (Character.isLowerCase(ch)) lower = true;
            else if (Character.isDigit(ch)) digit = true;
            else if (!Character.isWhitespace(ch)) symbol = true;
        }
        if (!(upper && lower && digit && symbol)) {
            return "Password must contain an upper-case letter, a lower-case letter, a digit and a symbol.";
        }
        return null;
    }

    /** @throws ValidationException when the text is empty or longer than maxLength */
    public static void requireText(String fieldName, String value, int maxLength) {
        if (isBlank(value)) {
            throw new ValidationException(fieldName + " must not be empty.");
        }
        if (value.trim().length() > maxLength) {
            throw new ValidationException(fieldName + " must be at most " + maxLength + " characters.");
        }
    }
}
