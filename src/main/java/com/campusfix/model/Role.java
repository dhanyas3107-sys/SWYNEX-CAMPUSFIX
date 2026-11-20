package com.campusfix.model;

/** The four kinds of account in CampusFix. */
public enum Role {
    STUDENT("Student"),
    FACULTY("Faculty"),
    ADMINISTRATOR("Administrator"),
    TECHNICIAN("Technician");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** @return human-readable name for menus and messages */
    public String getLabel() {
        return label;
    }
}
