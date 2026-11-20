package com.campusfix.exception;

import java.sql.SQLException;

/** A database operation failed. The message is user-safe; technical details stay in the cause. */
public class DatabaseException extends CampusFixException {
    private static final long serialVersionUID = 1L;


    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    private final boolean duplicateKey;

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.duplicateKey = cause instanceof SQLException sql && sql.getErrorCode() == MYSQL_DUPLICATE_ENTRY;
    }

    public DatabaseException(String message) {
        super(message);
        this.duplicateKey = false;
    }

    /** @return true when the failure was a UNIQUE constraint violation (e.g. duplicate email) */
    public boolean isDuplicateKey() {
        return duplicateKey;
    }
}
