package zw.ac.uz.dpdms.common.exception;

/** An approval action that is not allowed from the record's current status. Mapped to 409 Conflict. */
public class InvalidWorkflowTransitionException extends RuntimeException {
    public InvalidWorkflowTransitionException(String message) {
        super(message);
    }
}
