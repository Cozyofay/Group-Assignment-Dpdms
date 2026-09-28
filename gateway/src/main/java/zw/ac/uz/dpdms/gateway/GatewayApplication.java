package zw.ac.uz.dpdms.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single entry point for all clients (http://localhost:8080). Routes /api/** to the right
 * service via Eureka. It only routes: every service enforces its own security, so nothing
 * is protected "just" by the gateway.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
