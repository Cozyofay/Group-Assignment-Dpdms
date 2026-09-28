package zw.ac.uz.dpdms.drought.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DroughtIncidentRepository
        extends JpaRepository<DroughtIncident, Long>, JpaSpecificationExecutor<DroughtIncident> {
}
