package zw.ac.uz.dpdms.common.security;

/** Which records a user may see when listing a hazard's incidents. */
public enum ListScope {
    /** Every status (supervisor of that hazard, provincial admin). */
    ALL,
    /** Approved records plus the user's own non-approved records (ward recorder). */
    APPROVED_OR_OWN,
    /** Approved records only (national viewer). */
    APPROVED_ONLY
}
