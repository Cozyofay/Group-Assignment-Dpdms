package zw.ac.uz.dpdms.zoonotic.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ZoonoticDiseaseIncidentRepository
        extends JpaRepository<ZoonoticDiseaseIncident, Long>, JpaSpecificationExecutor<ZoonoticDiseaseIncident> {
}
