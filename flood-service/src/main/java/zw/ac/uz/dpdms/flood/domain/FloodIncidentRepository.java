package zw.ac.uz.dpdms.flood.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FloodIncidentRepository
        extends JpaRepository<FloodIncident, Long>, JpaSpecificationExecutor<FloodIncident> {
}
