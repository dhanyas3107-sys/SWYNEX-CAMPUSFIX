package com.campusfix.exception;

/** User input or a business rule check failed. The message is safe to show to the user. */
public class ValidationException extends CampusFixException {
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
