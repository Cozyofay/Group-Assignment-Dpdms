package zw.ac.uz.dpdms.common.exception;

/** Thrown when a user acts outside their hazard / ward / role scope. Mapped to 403 Forbidden. */
public class HazardAccessDeniedException extends RuntimeException {
    public HazardAccessDeniedException(String message) {
        super(message);
    }
}
