package zw.ac.uz.dpdms.fire.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FireIncidentRepository
        extends JpaRepository<FireIncident, Long>, JpaSpecificationExecutor<FireIncident> {
}
