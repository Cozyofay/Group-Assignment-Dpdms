package zw.ac.uz.dpdms.common.exception;

/** Mapped to 404 Not Found. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
