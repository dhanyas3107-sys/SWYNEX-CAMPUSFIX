package com.campusfix.exception;

import com.campusfix.util.IdGenerator;

/** No complaint exists with the requested id. */
public class ComplaintNotFoundException extends CampusFixException {
    private static final long serialVersionUID = 1L;

    public ComplaintNotFoundException(int complaintId) {
        super("Complaint " + IdGenerator.formatComplaintId(complaintId) + " was not found.");
    }
}
