package zw.ac.uz.dpdms.common.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.security.AuthenticatedUser;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditService {

    private final IncidentAuditLogRepository repository;

    public AuditService(IncidentAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(HazardType hazard, Long incidentId, AuditAction action,
                       IncidentStatus fromStatus, IncidentStatus toStatus,
                       AuthenticatedUser actor, String changes, String comment) {
        IncidentAuditLog entry = new IncidentAuditLog();
        entry.setHazard(hazard);
        entry.setIncidentId(incidentId);
        entry.setAction(action);
        entry.setFromStatus(fromStatus);
        entry.setToStatus(toStatus);
        entry.setActorUsername(actor.username());
        entry.setActorRole(actor.role());
        entry.setActedAt(LocalDateTime.now());
        entry.setChanges(changes);
        entry.setComment(comment);
        repository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<IncidentAuditLog> history(Long incidentId) {
        return repository.findByIncidentIdOrderByActedAtAsc(incidentId);
    }
}
