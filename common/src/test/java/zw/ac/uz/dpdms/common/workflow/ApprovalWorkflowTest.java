package zw.ac.uz.dpdms.common.workflow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.exception.InvalidWorkflowTransitionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static zw.ac.uz.dpdms.common.domain.IncidentStatus.APPROVED;
import static zw.ac.uz.dpdms.common.domain.IncidentStatus.CORRECTIONS_REQUESTED;
import static zw.ac.uz.dpdms.common.domain.IncidentStatus.PENDING;
import static zw.ac.uz.dpdms.common.domain.IncidentStatus.REJECTED;

class ApprovalWorkflowTest {

    @Test
    void supervisorDecisionsFromPending() {
        assertEquals(APPROVED, ApprovalWorkflow.apply(PENDING, WorkflowAction.APPROVE));
        assertEquals(REJECTED, ApprovalWorkflow.apply(PENDING, WorkflowAction.REJECT));
        assertEquals(CORRECTIONS_REQUESTED, ApprovalWorkflow.apply(PENDING, WorkflowAction.REQUEST_CORRECTIONS));
    }

    @Test
    void resubmitReturnsToPending() {
        assertEquals(PENDING, ApprovalWorkflow.apply(CORRECTIONS_REQUESTED, WorkflowAction.RESUBMIT));
    }

    @ParameterizedTest
    @EnumSource(WorkflowAction.class)
    void approvedAndRejectedAreFinal(WorkflowAction action) {
        assertThrows(InvalidWorkflowTransitionException.class, () -> ApprovalWorkflow.apply(APPROVED, action));
        assertThrows(InvalidWorkflowTransitionException.class, () -> ApprovalWorkflow.apply(REJECTED, action));
    }

    @Test
    void cannotApproveUntilResubmitted() {
        assertThrows(InvalidWorkflowTransitionException.class,
                () -> ApprovalWorkflow.apply(CORRECTIONS_REQUESTED, WorkflowAction.APPROVE));
    }

    @Test
    void cannotResubmitPending() {
        assertThrows(InvalidWorkflowTransitionException.class,
                () -> ApprovalWorkflow.apply(PENDING, WorkflowAction.RESUBMIT));
    }

    @Test
    void editability() {
        assertTrue(ApprovalWorkflow.isEditable(PENDING));
        assertTrue(ApprovalWorkflow.isEditable(CORRECTIONS_REQUESTED));
        assertFalse(ApprovalWorkflow.isEditable(APPROVED));
        assertFalse(ApprovalWorkflow.isEditable(REJECTED));
    }

    @Test
    void rejectAndCorrectionsNeedAReason() {
        assertThrows(IllegalArgumentException.class,
                () -> ApprovalWorkflow.validateComment(WorkflowAction.REJECT, "  "));
        assertThrows(IllegalArgumentException.class,
                () -> ApprovalWorkflow.validateComment(WorkflowAction.REQUEST_CORRECTIONS, null));
        assertDoesNotThrow(() -> ApprovalWorkflow.validateComment(WorkflowAction.APPROVE, null));
        assertDoesNotThrow(() -> ApprovalWorkflow.validateComment(WorkflowAction.REJECT, "Duplicate record"));
    }

    @Test
    void everyStatusIsCovered() {
        // Guards against someone adding a status without updating the workflow
        assertEquals(4, IncidentStatus.values().length);
    }
}
