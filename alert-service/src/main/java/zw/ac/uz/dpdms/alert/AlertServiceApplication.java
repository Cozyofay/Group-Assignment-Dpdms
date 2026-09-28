package zw.ac.uz.dpdms.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Listens for alert events on RabbitMQ and dispatches them by email and WhatsApp, asynchronously,
 * so that incident capture is never delayed. Every attempt is written to the alert log.
 */
@SpringBootApplication
public class AlertServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.class, args);
    }
}
