package com.campusfix.service;

import com.campusfix.dao.UserDAO;
import com.campusfix.database.DatabaseConnection;
import com.campusfix.exception.AuthenticationException;
import com.campusfix.model.User;
import com.campusfix.util.PasswordUtil;

import java.util.Optional;

/** Verifies email + password and returns the matching {@link User}. */
public class AuthenticationService {

    private static final String GENERIC_FAILURE = "Invalid email or password.";

    private final UserDAO userDAO;
    private String dummyHash; // created lazily, see verifyAgainstDummy()

    public AuthenticationService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * @throws AuthenticationException with the same generic message for an unknown email, a wrong
     *                                 password or a disabled account (so attackers learn nothing)
     */
    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            throw new AuthenticationException(GENERIC_FAILURE);
        }
        Optional<User> found = DatabaseConnection.read(c -> userDAO.findByEmail(c, email.trim().toLowerCase()));
        if (found.isEmpty()) {
            verifyAgainstDummy(password); // keeps response time similar to a real check
            throw new AuthenticationException(GENERIC_FAILURE);
        }
        User user = found.get();
        if (!PasswordUtil.verifyPassword(password, user.getPasswordHash()) || !user.isActive()) {
            throw new AuthenticationException(GENERIC_FAILURE);
        }
        return user;
    }

    private synchronized void verifyAgainstDummy(String password) {
        if (dummyHash == null) {
            dummyHash = PasswordUtil.hashPassword("not-a-real-password");
        }
        PasswordUtil.verifyPassword(password, dummyHash);
    }
}
