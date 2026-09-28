package zw.ac.uz.dpdms.alert.service;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.ac.uz.dpdms.alert.domain.AlertChannel;
import zw.ac.uz.dpdms.alert.domain.AlertLog;
import zw.ac.uz.dpdms.alert.domain.AlertLogRepository;
import zw.ac.uz.dpdms.alert.domain.DeliveryStatus;
import zw.ac.uz.dpdms.alert.dto.AlertLogResponse;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.security.AuthenticatedUser;
import zw.ac.uz.dpdms.common.security.CurrentUser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read access to the alert log. Hazard scoping still applies: a recorder or supervisor sees only
 * the alerts of their own hazard, while the admin and national users see everything.
 */
@Service
@Transactional(readOnly = true)
public class AlertLogService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "sentAt");

    private final AlertLogRepository repository;

    public AlertLogService(AlertLogRepository repository) {
        this.repository = repository;
    }

    public List<AlertLogResponse> list(HazardType hazard, AlertChannel channel, DeliveryStatus status,
                                       LocalDateTime from, LocalDateTime to) {
        AuthenticatedUser user = CurrentUser.get();
        HazardType effectiveHazard = user.hazard() != null ? user.hazard() : hazard;

        Specification<AlertLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (effectiveHazard != null) {
                predicates.add(cb.equal(root.get("hazard"), effectiveHazard));
            }
            if (channel != null) {
                predicates.add(cb.equal(root.get("channel"), channel));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("sentAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("sentAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return repository.findAll(spec, NEWEST_FIRST).stream().map(AlertLogResponse::from).toList();
    }

    public Map<String, Object> statistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", repository.count());
        stats.put("sent", repository.countByStatus(DeliveryStatus.SENT));
        stats.put("failed", repository.countByStatus(DeliveryStatus.FAILED));
        stats.put("skipped", repository.countByStatus(DeliveryStatus.SKIPPED));
        return stats;
    }
}
