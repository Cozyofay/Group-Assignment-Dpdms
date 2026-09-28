package zw.ac.uz.dpdms.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentAuditLogRepository extends JpaRepository<IncidentAuditLog, Long> {

    List<IncidentAuditLog> findByIncidentIdOrderByActedAtAsc(Long incidentId);
}
