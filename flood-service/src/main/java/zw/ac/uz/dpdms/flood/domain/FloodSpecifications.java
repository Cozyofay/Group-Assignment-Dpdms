package zw.ac.uz.dpdms.flood.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.security.AuthenticatedUser;
import zw.ac.uz.dpdms.common.security.HazardAccessPolicy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Turns the access rules and the query filters into database predicates. */
public final class FloodSpecifications {

    private FloodSpecifications() {
    }

    /**
     * The visibility rule, applied to EVERY list query so a pending record can never leak:
     * supervisors and the admin see all statuses, a recorder sees approved records plus their own,
     * national users see approved records only.
     */
    public static Specification<FloodIncident> visibleTo(AuthenticatedUser user) {
        return switch (HazardAccessPolicy.listScope(user)) {
            case ALL -> (root, query, cb) -> cb.conjunction();
            case APPROVED_ONLY -> (root, query, cb) -> cb.equal(root.get("status"), IncidentStatus.APPROVED);
            case APPROVED_OR_OWN -> (root, query, cb) -> cb.or(
                    cb.equal(root.get("status"), IncidentStatus.APPROVED),
                    cb.equal(root.get("reporterUsername"), user.username()));
        };
    }

    public static Specification<FloodIncident> filters(String ward, String district, IncidentStatus status,
                                                       Severity severity, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ward != null && !ward.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("ward")), ward.trim().toLowerCase()));
            }
            if (district != null && !district.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("district")), district.trim().toLowerCase()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<FloodIncident> approvedOnly() {
        return (root, query, cb) -> cb.equal(root.get("status"), IncidentStatus.APPROVED);
    }
}
