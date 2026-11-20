package com.campusfix.service;

import com.campusfix.dao.UserDAO;
import com.campusfix.database.DatabaseConnection;
import com.campusfix.exception.DatabaseException;
import com.campusfix.exception.ValidationException;
import com.campusfix.model.*;
import com.campusfix.util.PasswordUtil;
import com.campusfix.validation.InputValidator;
import com.campusfix.validation.UserValidator;

/** Registration of students and faculty, and the one-time creation of the first administrator. */
public class UserService {

    static final String DUPLICATE_EMAIL = "An account with this email address already exists.";

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public User registerStudent(String name, String email, String phone, String password) {
        return register(Role.STUDENT, name, email, phone, password);
    }

    public User registerFaculty(String name, String email, String phone, String password) {
        return register(Role.FACULTY, name, email, phone, password);
    }

    public boolean administratorExists() {
        return DatabaseConnection.read(userDAO::existsActiveAdministrator);
    }

    /**
     * Creates the very first administrator. Refuses to run if an active administrator already
     * exists, so it cannot be used later to create hidden admin accounts.
     */
    public User createInitialAdministrator(String name, String email, String phone, String password) {
        if (administratorExists()) {
            throw new ValidationException("An administrator account already exists. Initial setup is disabled.");
        }
        return register(Role.ADMINISTRATOR, name, email, phone, password);
    }

    private User register(Role role, String name, String email, String phone, String password) {
        UserValidator.validateRegistration(name, email, phone, password);
        String cleanEmail = email.trim().toLowerCase();
        String cleanName = name.trim();
        String cleanPhone = InputValidator.normalizePhone(phone);
        String hash = PasswordUtil.hashPassword(password);

        User user = switch (role) {
            case STUDENT -> new Student(0, cleanName, cleanEmail, hash, cleanPhone, null);
            case FACULTY -> new Faculty(0, cleanName, cleanEmail, hash, cleanPhone, null);
            case ADMINISTRATOR -> new Administrator(0, cleanName, cleanEmail, hash, cleanPhone, null);
            default -> throw new IllegalArgumentException("Use TechnicianService to create technicians.");
        };
        try {
            DatabaseConnection.transaction(conn -> {
                if (userDAO.existsByEmail(conn, cleanEmail)) {
                    throw new ValidationException(DUPLICATE_EMAIL);
                }
                userDAO.insert(conn, user);
                return null;
            });
        } catch (DatabaseException e) {
            if (e.isDuplicateKey()) { // two people registering the same email at the same moment
                throw new ValidationException(DUPLICATE_EMAIL);
            }
            throw e;
        }
        return user;
    }
}
