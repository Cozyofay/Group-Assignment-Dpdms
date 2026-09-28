package zw.ac.uz.dpdms.common.audit;

import zw.ac.uz.dpdms.common.workflow.WorkflowAction;

public enum AuditAction {
    CREATED,
    UPDATED,
    DELETED,
    APPROVED,
    REJECTED,
    CORRECTIONS_REQUESTED,
    RESUBMITTED;

    public static AuditAction from(WorkflowAction action) {
        return switch (action) {
            case APPROVE -> APPROVED;
            case REJECT -> REJECTED;
            case REQUEST_CORRECTIONS -> CORRECTIONS_REQUESTED;
            case RESUBMIT -> RESUBMITTED;
        };
    }
}
