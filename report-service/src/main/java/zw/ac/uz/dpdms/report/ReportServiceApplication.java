package zw.ac.uz.dpdms.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Reusable reporting capability. It owns no data: it asks each hazard service for its APPROVED
 * records and renders them as PDF, Word, Excel or CSV, so report logic is written once, not five times.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class ReportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportServiceApplication.class, args);
    }
}
