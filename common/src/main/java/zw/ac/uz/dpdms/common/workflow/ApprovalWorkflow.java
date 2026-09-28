package zw.ac.uz.dpdms.common.workflow;

import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.exception.InvalidWorkflowTransitionException;

/**
 * The approval state machine, shared by all five hazard services.
 *
 * <pre>
 *   (capture) --> PENDING --APPROVE-------------> APPROVED   (final)
 *                    |    --REJECT--------------> REJECTED   (final)
 *                    |    --REQUEST_CORRECTIONS-> CORRECTIONS_REQUESTED
 *                    |                                   |
 *                    +<----------- RESUBMIT -------------+
 * </pre>
 *
 * Who may perform each action is decided separately by HazardAccessPolicy.
 */
public final class ApprovalWorkflow {

    private ApprovalWorkflow() {
    }

    /** Returns the new status, or throws if the action is not allowed from the current status. */
    public static IncidentStatus apply(IncidentStatus current, WorkflowAction action) {
        return switch (action) {
            case APPROVE -> requireFrom(current, IncidentStatus.PENDING, IncidentStatus.APPROVED, action);
            case REJECT -> requireFrom(current, IncidentStatus.PENDING, IncidentStatus.REJECTED, action);
            case REQUEST_CORRECTIONS ->
                    requireFrom(current, IncidentStatus.PENDING, IncidentStatus.CORRECTIONS_REQUESTED, action);
            case RESUBMIT ->
                    requireFrom(current, IncidentStatus.CORRECTIONS_REQUESTED, IncidentStatus.PENDING, action);
        };
    }

    /** Throws if the action needs a reason (reject / request corrections) and none was given. */
    public static void validateComment(WorkflowAction action, String comment) {
        if (action.requiresComment() && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("A comment explaining the reason is required to "
                    + action.name().toLowerCase().replace('_', ' '));
        }
    }

    /** Recorders may only edit a record before a final decision is made. */
    public static boolean isEditable(IncidentStatus status) {
        return status == IncidentStatus.PENDING || status == IncidentStatus.CORRECTIONS_REQUESTED;
    }

    private static IncidentStatus requireFrom(IncidentStatus current, IncidentStatus expected,
                                              IncidentStatus next, WorkflowAction action) {
        if (current != expected) {
            throw new InvalidWorkflowTransitionException(
                    "Cannot " + action.name() + " a record that is " + current + " (it must be " + expected + ")");
        }
        return next;
    }
}
