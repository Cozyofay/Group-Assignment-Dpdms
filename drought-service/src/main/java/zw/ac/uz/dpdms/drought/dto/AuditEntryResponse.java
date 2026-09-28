package zw.ac.uz.dpdms.drought.dto;

import zw.ac.uz.dpdms.common.audit.AuditAction;
import zw.ac.uz.dpdms.common.audit.IncidentAuditLog;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Role;

import java.time.LocalDateTime;

/** One line of the audit trail: who acted, when, and what changed. */
public record AuditEntryResponse(Long id, AuditAction action, IncidentStatus fromStatus, IncidentStatus toStatus,
                                 String actorUsername, Role actorRole, LocalDateTime actedAt,
                                 String changes, String comment) {

    public static AuditEntryResponse from(IncidentAuditLog entry) {
        return new AuditEntryResponse(entry.getId(), entry.getAction(), entry.getFromStatus(), entry.getToStatus(),
                entry.getActorUsername(), entry.getActorRole(), entry.getActedAt(), entry.getChanges(),
                entry.getComment());
    }
}
