package zw.ac.uz.dpdms.common.dto;

/** Input whitelists that block HTML/script injection (XSS) at the API boundary. */
public final class ValidationPatterns {

    /** Letters (any language), digits, spaces and basic punctuation. Used for names like ward or basin. */
    public static final String SAFE_TEXT = "^[\\p{L}\\p{N} .,'()/&-]*$";
    public static final String SAFE_TEXT_MESSAGE = "contains characters that are not allowed";

    /** Free text (comments, reasons): anything except angle brackets. */
    public static final String SAFE_LONG_TEXT = "^[^<>]*$";
    public static final String SAFE_LONG_TEXT_MESSAGE = "must not contain < or >";

    private ValidationPatterns() {
    }
}
