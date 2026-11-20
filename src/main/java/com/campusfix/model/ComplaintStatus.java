package com.campusfix.model;

import com.campusfix.exception.InvalidStatusTransitionException;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle of a complaint. The allowed moves are defined in ONE place
 * ({@link #allowedNext()}) so the whole application shares the same rules.
 *
 * <pre>
 * SUBMITTED -> UNDER_REVIEW -> ASSIGNED -> IN_PROGRESS -> RESOLVED
 *     |             |             ^             |
 *     +-> REJECTED  +-> REJECTED   +-------------+  (IN_PROGRESS -> ASSIGNED only when reassigned)
 *     +-> CANCELLED +-> CANCELLED
 * </pre>
 * RESOLVED, REJECTED and CANCELLED are terminal: nothing can follow them.
 */
public enum ComplaintStatus {
    SUBMITTED, UNDER_REVIEW, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED, CANCELLED;

    /** @return the statuses that may directly follow this one */
    public Set<ComplaintStatus> allowedNext() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(UNDER_REVIEW, REJECTED, CANCELLED);
            case UNDER_REVIEW -> EnumSet.of(ASSIGNED, REJECTED, CANCELLED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(RESOLVED, ASSIGNED);
            case RESOLVED, REJECTED, CANCELLED -> EnumSet.noneOf(ComplaintStatus.class);
        };
    }

    public boolean canTransitionTo(ComplaintStatus target) {
        return allowedNext().contains(target);
    }

    /** @throws InvalidStatusTransitionException if the move is not allowed */
    public void requireTransitionTo(ComplaintStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(this, target);
        }
    }

    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }

    /** A user may cancel only before a technician has been assigned. */
    public boolean isCancellable() {
        return canTransitionTo(CANCELLED);
    }

    /** "Pending" = reported but work has not started yet. */
    public boolean isPending() {
        return this == SUBMITTED || this == UNDER_REVIEW || this == ASSIGNED;
    }
}
