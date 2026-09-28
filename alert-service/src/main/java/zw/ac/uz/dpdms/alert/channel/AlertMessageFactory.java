package zw.ac.uz.dpdms.alert.channel;

import org.springframework.stereotype.Component;
import zw.ac.uz.dpdms.common.messaging.AlertEvent;

import java.time.format.DateTimeFormatter;

/** Builds the wording of the alert, shared by both channels. */
@Component
public class AlertMessageFactory {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public String subject(AlertEvent event) {
        return "[DPDMS " + event.severity() + "] " + event.hazard().getLabel() + " alert - " + event.ward();
    }

    public String body(AlertEvent event, String recipientName) {
        return """
                Dear %s,

                A %s incident has been reported and meets the alerting criteria.

                Reason for this alert : %s
                Ward / District       : %s, %s
                Occurred              : %s
                Severity              : %s
                Summary               : %s
                Location (lat, lon)   : %s, %s
                Incident reference    : %s #%d

                Open the DPDMS dashboard for the full record and the map.

                This is an automated message from the Provincial Disaster Management Office.
                """.formatted(recipientName, event.hazard().getLabel(), event.reason(), event.ward(),
                event.district(), event.occurredAt().format(WHEN), event.severity(), event.headline(),
                event.latitude(), event.longitude(), event.hazard().getLabel(), event.incidentId());
    }

    /** WhatsApp messages are short, so they stay readable on a phone. */
    public String whatsAppText(AlertEvent event) {
        return "*DPDMS " + event.severity() + " " + event.hazard().getLabel() + " ALERT*\n"
                + event.ward() + ", " + event.district() + " - " + event.occurredAt().format(WHEN) + "\n"
                + event.headline() + "\n"
                + "Reason: " + event.reason() + "\n"
                + "Location: " + event.latitude() + ", " + event.longitude();
    }
}
