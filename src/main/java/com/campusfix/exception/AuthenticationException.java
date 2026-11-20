package com.campusfix.exception;

/** Login failed. The message is deliberately generic to avoid revealing which part was wrong. */
public class AuthenticationException extends CampusFixException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
