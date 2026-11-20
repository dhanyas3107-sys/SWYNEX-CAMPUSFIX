package com.campusfix.database;

import com.campusfix.exception.DatabaseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The single place where JDBC connections are created.
 *
 * <p>Credentials come from environment variables, never from source code:
 * <ul>
 *   <li>{@code CAMPUSFIX_DB_URL} (optional, default {@code jdbc:mysql://localhost:3306/campusfix})</li>
 *   <li>{@code CAMPUSFIX_DB_USER} (required)</li>
 *   <li>{@code CAMPUSFIX_DB_PASSWORD} (required)</li>
 * </ul>
 *
 * <p>Services use {@link #read(SqlWork)} for queries and {@link #transaction(SqlWork)} for
 * operations that change several rows. Both close the connection automatically (try-with-resources),
 * and the transaction helper commits on success and rolls back on any failure.
 */
public final class DatabaseConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/campusfix?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private DatabaseConnection() { }

    /** A piece of database work that receives an open connection. */
    @FunctionalInterface
    public interface SqlWork<T> {
        T execute(Connection connection) throws SQLException;
    }

    /** Opens a new connection. The caller must close it. */
    public static Connection getConnection() {
        String url = valueOrDefault("CAMPUSFIX_DB_URL", DEFAULT_URL);
        String user = required("CAMPUSFIX_DB_USER");
        String password = required("CAMPUSFIX_DB_PASSWORD");
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException("Could not connect to the database. Check that MySQL is running and that "
                    + "CAMPUSFIX_DB_URL, CAMPUSFIX_DB_USER and CAMPUSFIX_DB_PASSWORD are correct.", e);
        }
    }

    /** Runs read-only work on a fresh connection. */
    public static <T> T read(SqlWork<T> work) {
        try (Connection connection = getConnection()) {
            return work.execute(connection);
        } catch (SQLException e) {
            throw new DatabaseException("A database error occurred. Please try again.", e);
        }
    }

    /** Runs work as one atomic transaction: everything is saved, or nothing is. */
    public static <T> T transaction(SqlWork<T> work) {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = work.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(connection);
                if (e instanceof SQLException sql) {
                    throw new DatabaseException("A database error occurred. No changes were saved.", sql);
                }
                throw (RuntimeException) e;
            }
        } catch (SQLException e) {
            throw new DatabaseException("A database error occurred. Please try again.", e);
        }
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // the original failure is more important than a failed rollback
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null) {
            throw new DatabaseException("Environment variable " + name + " is not set. See the README (section 'Environment variables').");
        }
        return value;
    }

    private static String valueOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
