package zw.ac.uz.dpdms.alert.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import zw.ac.uz.dpdms.alert.service.AlertDispatchService;
import zw.ac.uz.dpdms.common.messaging.AlertEvent;
import zw.ac.uz.dpdms.common.messaging.AlertMessaging;

/**
 * Consumes alert events published by the five hazard services. Because this runs on a queue,
 * sending email and WhatsApp never blocks incident capture.
 */
@Component
public class AlertEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertEventListener.class);

    private final AlertDispatchService dispatchService;

    public AlertEventListener(AlertDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @RabbitListener(queues = AlertMessaging.QUEUE)
    public void onAlert(AlertEvent event) {
        log.info("Received alert event {} for {} incident {}", event.eventId(), event.hazard(), event.incidentId());
        dispatchService.dispatch(event);
    }
}
