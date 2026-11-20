package com.campusfix.model;

import java.time.LocalDateTime;

/**
 * One immutable row in a complaint's audit trail. When the previous and new status are equal
 * the row is a note (progress note, priority change, reassignment, edit).
 */
public class ComplaintHistory {

    private final int historyId;
    private final int complaintId;
    private final ComplaintStatus previousStatus; // null for the very first entry
    private final ComplaintStatus newStatus;
    private final int updatedBy;
    private final String updatedByName;
    private final LocalDateTime changedAt;
    private final String remarks;

    /** Used when creating a new entry (id, name and timestamp are filled in by the database). */
    public ComplaintHistory(int complaintId, ComplaintStatus previousStatus,
                            ComplaintStatus newStatus, int updatedBy, String remarks) {
        this(0, complaintId, previousStatus, newStatus, updatedBy, null, null, remarks);
    }

    /** Used when loading an entry from the database. */
    public ComplaintHistory(int historyId, int complaintId, ComplaintStatus previousStatus,
                            ComplaintStatus newStatus, int updatedBy, String updatedByName,
                            LocalDateTime changedAt, String remarks) {
        this.historyId = historyId;
        this.complaintId = complaintId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.updatedBy = updatedBy;
        this.updatedByName = updatedByName;
        this.changedAt = changedAt;
        this.remarks = remarks;
    }

    public int getHistoryId() { return historyId; }
    public int getComplaintId() { return complaintId; }
    public ComplaintStatus getPreviousStatus() { return previousStatus; }
    public ComplaintStatus getNewStatus() { return newStatus; }
    public int getUpdatedBy() { return updatedBy; }
    public String getUpdatedByName() { return updatedByName; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public String getRemarks() { return remarks; }
}
