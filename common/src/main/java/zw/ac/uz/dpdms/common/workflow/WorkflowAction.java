package zw.ac.uz.dpdms.common.workflow;

public enum WorkflowAction {
    APPROVE(false),
    REJECT(true),
    REQUEST_CORRECTIONS(true),
    RESUBMIT(false);

    private final boolean requiresComment;

    WorkflowAction(boolean requiresComment) {
        this.requiresComment = requiresComment;
    }

    public boolean requiresComment() {
        return requiresComment;
    }
}
