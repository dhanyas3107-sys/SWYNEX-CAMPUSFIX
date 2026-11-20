package com.campusfix.exception;

/** The logged-in user is not allowed to perform the requested operation. */
public class UnauthorizedOperationException extends CampusFixException {
    private static final long serialVersionUID = 1L;

    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
