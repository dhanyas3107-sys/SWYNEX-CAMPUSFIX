package com.campusfix.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** Manages complaints, technicians and reports. */
public class Administrator extends User {

    private static final Set<Permission> PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(
            Permission.VIEW_ALL_COMPLAINTS, Permission.REVIEW_COMPLAINT,
            Permission.ASSIGN_TECHNICIAN, Permission.CHANGE_PRIORITY,
            Permission.MANAGE_TECHNICIANS, Permission.GENERATE_REPORTS));

    public Administrator(int userId, String name, String email, String passwordHash,
                         String phoneNumber, LocalDateTime createdAt) {
        super(userId, name, email, passwordHash, phoneNumber, createdAt);
    }

    @Override
    public Role getRole() {
        return Role.ADMINISTRATOR;
    }

    @Override
    public Set<Permission> getPermissions() {
        return PERMISSIONS;
    }

    @Override
    public String getDashboardTitle() {
        return "ADMINISTRATOR CONTROL PANEL";
    }
}
