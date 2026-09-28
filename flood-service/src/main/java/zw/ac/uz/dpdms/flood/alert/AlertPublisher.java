package zw.ac.uz.dpdms.flood.alert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import zw.ac.uz.dpdms.common.messaging.AlertEvent;
import zw.ac.uz.dpdms.common.messaging.AlertMessaging;

/**
 * Publishes alert events to RabbitMQ so the alert-service can send email and WhatsApp
 * asynchronously. Capture must never fail because messaging is down, so every failure here is
 * logged and swallowed (reliability requirement: degrade gracefully).
 */
@Component
public class AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);

    private final ObjectProvider<RabbitTemplate> rabbitTemplate;
    private final boolean enabled;

    public AlertPublisher(ObjectProvider<RabbitTemplate> rabbitTemplate,
                          @Value("${dpdms.alerts.enabled:true}") boolean enabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.enabled = enabled;
    }

    public void publish(AlertEvent event) {
        if (!enabled) {
            return;
        }
        RabbitTemplate template = rabbitTemplate.getIfAvailable();
        if (template == null) {
            log.debug("RabbitMQ is not configured; alert {} was not queued", event.eventId());
            return;
        }
        try {
            template.convertAndSend(AlertMessaging.EXCHANGE, AlertMessaging.ROUTING_KEY, event);
            log.info("Queued flood alert for incident {} ({})", event.incidentId(), event.reason());
        } catch (AmqpException ex) {
            log.warn("Could not queue alert for incident {}: {}. Incident capture is unaffected.",
                    event.incidentId(), ex.getMessage());
        }
    }
}
