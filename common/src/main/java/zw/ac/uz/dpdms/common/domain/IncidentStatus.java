package zw.ac.uz.dpdms.common.domain;

/** Approval workflow states. Only APPROVED records reach the dashboard, map and reports. */
public enum IncidentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CORRECTIONS_REQUESTED
}
