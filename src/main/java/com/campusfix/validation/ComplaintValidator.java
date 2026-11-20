package com.campusfix.validation;

import com.campusfix.exception.ValidationException;
import com.campusfix.model.Category;
import com.campusfix.model.Priority;

import java.util.ArrayList;
import java.util.List;

/** Validates complaint input and free-text remarks. */
public final class ComplaintValidator {

    public static final int MIN_DESCRIPTION_LENGTH = 10;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_PLACE_LENGTH = 100;
    public static final int MAX_REMARKS_LENGTH = 500;

    private ComplaintValidator() { }

    /** Validates every field of a new (or edited) complaint and reports all problems at once. */
    public static void validateComplaint(Category category, String description,
                                         String building, String location, Priority priority) {
        List<String> errors = new ArrayList<>();
        if (category == null) {
            errors.add("Category must be selected.");
        }
        if (InputValidator.isBlank(description)) {
            errors.add("Description must not be empty.");
        } else if (description.trim().length() < MIN_DESCRIPTION_LENGTH) {
            errors.add("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters so technicians can understand the problem.");
        } else if (description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            errors.add("Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters.");
        }
        checkPlace("Building", building, errors);
        checkPlace("Location", location, errors);
        if (priority == null) {
            errors.add("Priority must be selected.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(String.join(" ", errors));
        }
    }

    /** Converts text such as "plumbing" to {@link Category}. */
    public static Category parseCategory(String text) {
        try {
            return Category.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Invalid category: '" + text + "'.");
        }
    }

    /** Converts text such as "high" to {@link Priority}. */
    public static Priority parsePriority(String text) {
        try {
            return Priority.valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Invalid priority: '" + text + "'.");
        }
    }

    /** For remarks that are required (rejection reason, resolution notes, priority justification). */
    public static void requireRemarks(String fieldName, String text) {
        InputValidator.requireText(fieldName, text, MAX_REMARKS_LENGTH);
    }

    /** For remarks that may be left empty but must not be too long. */
    public static void validateOptionalRemarks(String text) {
        if (text != null && text.trim().length() > MAX_REMARKS_LENGTH) {
            throw new ValidationException("Remarks must be at most " + MAX_REMARKS_LENGTH + " characters.");
        }
    }

    private static void checkPlace(String field, String value, List<String> errors) {
        if (InputValidator.isBlank(value)) {
            errors.add(field + " must not be empty.");
        } else if (value.trim().length() > MAX_PLACE_LENGTH) {
            errors.add(field + " must be at most " + MAX_PLACE_LENGTH + " characters.");
        }
    }
}
