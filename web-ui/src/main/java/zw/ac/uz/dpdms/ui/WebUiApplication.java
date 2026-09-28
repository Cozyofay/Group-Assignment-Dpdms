package zw.ac.uz.dpdms.ui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * The user-facing application (Thymeleaf). It holds no data and enforces nothing by itself:
 * it logs in through the auth-service, keeps the JWT in the user's server-side session, and calls
 * the API gateway for everything. All authority checks happen in the backend services.
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class WebUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebUiApplication.class, args);
    }
}
