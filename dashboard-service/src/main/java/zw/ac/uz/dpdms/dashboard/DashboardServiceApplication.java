package zw.ac.uz.dpdms.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Read-only aggregation service: counts, trends, recent incidents and map points, built from the
 * APPROVED records of the five hazard services. It owns no database of its own.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class DashboardServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardServiceApplication.class, args);
    }
}
