package com.campusfix;

import com.campusfix.exception.InvalidStatusTransitionException;
import com.campusfix.model.ComplaintStatus;
import org.junit.jupiter.api.Test;

import static com.campusfix.model.ComplaintStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class ComplaintStatusTest {

    @Test
    void normalWorkflowIsAllowed() {
        assertTrue(SUBMITTED.canTransitionTo(UNDER_REVIEW));
        assertTrue(UNDER_REVIEW.canTransitionTo(ASSIGNED));
        assertTrue(ASSIGNED.canTransitionTo(IN_PROGRESS));
        assertTrue(IN_PROGRESS.canTransitionTo(RESOLVED));
    }

    @Test
    void stepsCannotBeSkipped() {
        assertFalse(SUBMITTED.canTransitionTo(ASSIGNED));
        assertFalse(SUBMITTED.canTransitionTo(IN_PROGRESS));
        assertFalse(SUBMITTED.canTransitionTo(RESOLVED));
        assertFalse(UNDER_REVIEW.canTransitionTo(RESOLVED));
        assertFalse(ASSIGNED.canTransitionTo(RESOLVED));
    }

    @Test
    void cannotMoveBackwardsExceptReassignment() {
        assertFalse(UNDER_REVIEW.canTransitionTo(SUBMITTED));
        assertFalse(ASSIGNED.canTransitionTo(UNDER_REVIEW));
        assertFalse(IN_PROGRESS.canTransitionTo(UNDER_REVIEW));
        assertTrue(IN_PROGRESS.canTransitionTo(ASSIGNED)); // reassignment
    }

    @Test
    void terminalStatusesHaveNoNextStatus() {
        for (ComplaintStatus terminal : new ComplaintStatus[]{RESOLVED, REJECTED, CANCELLED}) {
            assertTrue(terminal.isTerminal());
            for (ComplaintStatus target : ComplaintStatus.values()) {
                assertFalse(terminal.canTransitionTo(target), terminal + " -> " + target);
            }
        }
    }

    @Test
    void nonTerminalStatusesAreNotTerminal() {
        assertFalse(SUBMITTED.isTerminal());
        assertFalse(IN_PROGRESS.isTerminal());
    }

    @Test
    void cancellationOnlyBeforeAssignment() {
        assertTrue(SUBMITTED.isCancellable());
        assertTrue(UNDER_REVIEW.isCancellable());
        assertFalse(ASSIGNED.isCancellable());
        assertFalse(IN_PROGRESS.isCancellable());
        assertFalse(RESOLVED.isCancellable());
    }

    @Test
    void rejectionOnlyBeforeAssignment() {
        assertTrue(SUBMITTED.canTransitionTo(REJECTED));
        assertTrue(UNDER_REVIEW.canTransitionTo(REJECTED));
        assertFalse(ASSIGNED.canTransitionTo(REJECTED));
        assertFalse(IN_PROGRESS.canTransitionTo(REJECTED));
    }

    @Test
    void invalidTransitionThrowsWithClearMessage() {
        InvalidStatusTransitionException e = assertThrows(InvalidStatusTransitionException.class,
                () -> SUBMITTED.requireTransitionTo(RESOLVED));
        assertTrue(e.getMessage().contains("SUBMITTED"));
        assertTrue(e.getMessage().contains("RESOLVED"));
        assertThrows(InvalidStatusTransitionException.class, () -> CANCELLED.requireTransitionTo(SUBMITTED));
        assertDoesNotThrow(() -> SUBMITTED.requireTransitionTo(UNDER_REVIEW));
    }

    @Test
    void pendingStatuses() {
        assertTrue(SUBMITTED.isPending());
        assertTrue(UNDER_REVIEW.isPending());
        assertTrue(ASSIGNED.isPending());
        assertFalse(IN_PROGRESS.isPending());
        assertFalse(RESOLVED.isPending());
    }
}
