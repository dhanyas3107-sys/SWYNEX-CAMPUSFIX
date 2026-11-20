package com.campusfix.exception;

import com.campusfix.model.ComplaintStatus;

/** A complaint was asked to move to a status that the workflow does not allow. */
public class InvalidStatusTransitionException extends CampusFixException {
    private static final long serialVersionUID = 1L;


    public InvalidStatusTransitionException(ComplaintStatus from, ComplaintStatus to) {
        super("Invalid status change: " + from + " -> " + to + ". Allowed next statuses from "
                + from + ": " + (from.isTerminal() ? "none (final status)" : from.allowedNext()) + ".");
    }

    public InvalidStatusTransitionException(ComplaintStatus from, ComplaintStatus to, String reason) {
        super("Invalid status change: " + from + " -> " + to + ". " + reason);
    }
}
