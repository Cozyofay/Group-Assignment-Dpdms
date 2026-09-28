package zw.ac.uz.dpdms.ui.client;

/** An error returned by a backend service, with the message the service gave (e.g. a 403 reason). */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
