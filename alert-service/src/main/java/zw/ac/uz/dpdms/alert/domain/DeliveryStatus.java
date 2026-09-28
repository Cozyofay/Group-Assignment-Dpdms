package zw.ac.uz.dpdms.alert.domain;

public enum DeliveryStatus {
    /** Handed over to the mail server or WhatsApp gateway successfully. */
    SENT,
    /** The channel rejected it; the reason is in the log entry. */
    FAILED,
    /** The channel is switched off, or the recipient has no address for it. */
    SKIPPED
}
