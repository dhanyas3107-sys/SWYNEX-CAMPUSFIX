package com.campusfix.model;

import java.time.LocalDateTime;

/** A maintenance complaint reported by a student or faculty member. */
public class Complaint {

    private int complaintId;
    private int submittedBy;
    private String submittedByName;
    private Category category;
    private String description;
    private String building;
    private String location;
    private Priority priority;
    private ComplaintStatus status = ComplaintStatus.SUBMITTED;
    private Integer assignedTechnicianId;
    private String assignedTechnicianName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public boolean isOwnedBy(int userId) {
        return submittedBy == userId;
    }

    /** Details may only be edited while nobody has started working on the complaint. */
    public boolean isEditable() {
        return status == ComplaintStatus.SUBMITTED;
    }

    public boolean isCancellable() {
        return status.isCancellable();
    }

    public int getComplaintId() { return complaintId; }
    public void setComplaintId(int complaintId) { this.complaintId = complaintId; }
    public int getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(int submittedBy) { this.submittedBy = submittedBy; }
    public String getSubmittedByName() { return submittedByName; }
    public void setSubmittedByName(String submittedByName) { this.submittedByName = submittedByName; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public Integer getAssignedTechnicianId() { return assignedTechnicianId; }
    public void setAssignedTechnicianId(Integer assignedTechnicianId) { this.assignedTechnicianId = assignedTechnicianId; }
    public String getAssignedTechnicianName() { return assignedTechnicianName; }
    public void setAssignedTechnicianName(String assignedTechnicianName) { this.assignedTechnicianName = assignedTechnicianName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}
