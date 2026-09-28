package zw.ac.uz.dpdms.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Role;

import java.time.LocalDateTime;

/**
 * Audit trail: who acted, when, and what changed. Each hazard service stores its own copy of
 * this table in its own database.
 */
@Entity
@Table(name = "incident_audit_log", indexes = @Index(name = "idx_audit_incident", columnList = "incident_id"))
public class IncidentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HazardType hazard;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private IncidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 30)
    private IncidentStatus toStatus;

    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 30)
    private Role actorRole;

    @Column(name = "acted_at", nullable = false)
    private LocalDateTime actedAt;

    @Column(columnDefinition = "TEXT")
    private String changes;

    @Column(length = 1000)
    private String comment;

    public Long getId() { return id; }

    public HazardType getHazard() { return hazard; }
    public void setHazard(HazardType hazard) { this.hazard = hazard; }

    public Long getIncidentId() { return incidentId; }
    public void setIncidentId(Long incidentId) { this.incidentId = incidentId; }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public IncidentStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(IncidentStatus fromStatus) { this.fromStatus = fromStatus; }

    public IncidentStatus getToStatus() { return toStatus; }
    public void setToStatus(IncidentStatus toStatus) { this.toStatus = toStatus; }

    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }

    public Role getActorRole() { return actorRole; }
    public void setActorRole(Role actorRole) { this.actorRole = actorRole; }

    public LocalDateTime getActedAt() { return actedAt; }
    public void setActedAt(LocalDateTime actedAt) { this.actedAt = actedAt; }

    public String getChanges() { return changes; }
    public void setChanges(String changes) { this.changes = changes; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
