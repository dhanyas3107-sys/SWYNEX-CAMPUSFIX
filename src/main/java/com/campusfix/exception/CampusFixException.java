package com.campusfix.exception;

/**
 * Base class of all application-specific exceptions. It is unchecked (a RuntimeException) so
 * that business code stays readable; the console layer catches it once and shows the message.
 */
public class CampusFixException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CampusFixException(String message) {
        super(message);
    }

    public CampusFixException(String message, Throwable cause) {
        super(message, cause);
    }
}
