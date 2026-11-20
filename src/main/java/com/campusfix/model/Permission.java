package com.campusfix.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Fine-grained operations used for role-based access control (RBAC).
 * Each {@link User} subclass declares which permissions it owns, and
 * services check them before doing any work.
 */
public enum Permission {
    SUBMIT_COMPLAINT,
    VIEW_OWN_COMPLAINTS,
    EDIT_OWN_COMPLAINT,
    CANCEL_OWN_COMPLAINT,

    VIEW_ALL_COMPLAINTS,
    REVIEW_COMPLAINT,
    ASSIGN_TECHNICIAN,
    CHANGE_PRIORITY,
    MANAGE_TECHNICIANS,
    GENERATE_REPORTS,

    VIEW_ASSIGNED_COMPLAINTS,
    UPDATE_ASSIGNED_COMPLAINTS;

    /** Permissions shared by students and faculty (the people who report problems). */
    public static Set<Permission> submitterSet() {
        return Collections.unmodifiableSet(EnumSet.of(SUBMIT_COMPLAINT, VIEW_OWN_COMPLAINTS,
                EDIT_OWN_COMPLAINT, CANCEL_OWN_COMPLAINT));
    }
}
