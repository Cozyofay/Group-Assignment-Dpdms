package zw.ac.uz.dpdms.mining.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MiningAccidentIncidentRepository
        extends JpaRepository<MiningAccidentIncident, Long>, JpaSpecificationExecutor<MiningAccidentIncident> {
}
