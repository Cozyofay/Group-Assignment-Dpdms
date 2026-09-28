package zw.ac.uz.dpdms.drought;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Owns Drought incidents: its own database (dpdms_drought), its own REST API and its own build artifact.
 * The shared audit trail lives in zw.ac.uz.dpdms.common.audit, so that package is scanned too.
 */
@SpringBootApplication(scanBasePackages = {"zw.ac.uz.dpdms.drought", "zw.ac.uz.dpdms.common.audit"})
@EntityScan(basePackages = {"zw.ac.uz.dpdms.drought.domain", "zw.ac.uz.dpdms.common.audit"})
@EnableJpaRepositories(basePackages = {"zw.ac.uz.dpdms.drought.domain", "zw.ac.uz.dpdms.common.audit"})
public class DroughtServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DroughtServiceApplication.class, args);
    }
}
