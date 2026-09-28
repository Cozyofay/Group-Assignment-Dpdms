package zw.ac.uz.dpdms.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Issues signed JWTs and manages users. Every other service verifies those tokens itself
 * using the shared secret, so the auth-service is not called on every request.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
