package com.campusfix.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/** A maintenance worker. Extends {@link User} with specialization and availability. */
public class Technician extends User {

    private static final Set<Permission> PERMISSIONS = Collections.unmodifiableSet(EnumSet.of(
            Permission.VIEW_ASSIGNED_COMPLAINTS, Permission.UPDATE_ASSIGNED_COMPLAINTS));

    private int technicianId;
    private final Category specialization;
    private final String contactInfo;
    private Availability availability;

    public Technician(int userId, String name, String email, String passwordHash,
                      String phoneNumber, LocalDateTime createdAt,
                      int technicianId, Category specialization,
                      String contactInfo, Availability availability) {
        super(userId, name, email, passwordHash, phoneNumber, createdAt);
        this.technicianId = technicianId;
        this.specialization = specialization;
        this.contactInfo = contactInfo;
        this.availability = availability;
    }

    @Override
    public Role getRole() {
        return Role.TECHNICIAN;
    }

    @Override
    public Set<Permission> getPermissions() {
        return PERMISSIONS;
    }

    @Override
    public String getDashboardTitle() {
        return "TECHNICIAN WORKBENCH (" + specialization.getLabel() + ")";
    }

    public int getTechnicianId() { return technicianId; }
    public void setTechnicianId(int technicianId) { this.technicianId = technicianId; }
    public Category getSpecialization() { return specialization; }
    public String getContactInfo() { return contactInfo; }
    public Availability getAvailability() { return availability; }
    public void setAvailability(Availability availability) { this.availability = availability; }
}
