package com.campusfix.model;

/** Complaint categories. Also used as a technician's specialization. */
public enum Category {
    ELECTRICAL("Electrical"),
    PLUMBING("Plumbing"),
    NETWORK("Network / Wi-Fi"),
    CLEANLINESS("Cleanliness"),
    FURNITURE("Furniture"),
    LAB_EQUIPMENT("Lab Equipment"),
    OTHER("Other");

    private final String label;

    Category(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
