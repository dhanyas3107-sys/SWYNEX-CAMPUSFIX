package com.campusfix.model;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Abstract base class of every account.
 *
 * <p>OOP concepts: <b>abstraction</b> (cannot be instantiated; subclasses must say what
 * role/permissions they have), <b>encapsulation</b> (private fields, the password hash is
 * never included in {@link #toString()}), <b>polymorphism</b> (callers use {@code User}
 * and the right subclass behaviour runs).
 */
public abstract class User {

    private int userId;
    private final String name;
    private final String email;
    private final String passwordHash;
    private final String phoneNumber;
    private final LocalDateTime createdAt;
    private boolean active = true;

    protected User(int userId, String name, String email, String passwordHash,
                   String phoneNumber, LocalDateTime createdAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt;
    }

    /** @return the role of this account (each subclass overrides this) */
    public abstract Role getRole();

    /** @return the operations this kind of account may perform (each subclass overrides this) */
    public abstract Set<Permission> getPermissions();

    public boolean hasPermission(Permission permission) {
        return getPermissions().contains(permission);
    }

    /** Title shown on the user's menu. Subclasses may override. */
    public String getDashboardTitle() {
        return getRole().getLabel().toUpperCase() + " MENU";
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Needed only by authentication and by the DAO when saving. Never print this. */
    public String getPasswordHash() { return passwordHash; }

    @Override
    public String toString() {
        return getRole().getLabel() + "{id=" + userId + ", name=" + name + ", email=" + email + "}";
    }
}
