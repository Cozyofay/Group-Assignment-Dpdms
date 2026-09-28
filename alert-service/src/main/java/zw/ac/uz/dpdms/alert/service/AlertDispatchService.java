package zw.ac.uz.dpdms.alert.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import zw.ac.uz.dpdms.alert.channel.AlertMessageFactory;
import zw.ac.uz.dpdms.alert.channel.EmailSender;
import zw.ac.uz.dpdms.alert.channel.WhatsAppSender;
import zw.ac.uz.dpdms.alert.domain.AlertChannel;
import zw.ac.uz.dpdms.alert.domain.AlertLog;
import zw.ac.uz.dpdms.alert.domain.AlertLogRepository;
import zw.ac.uz.dpdms.alert.domain.DeliveryStatus;
import zw.ac.uz.dpdms.common.dto.AlertRecipient;
import zw.ac.uz.dpdms.common.messaging.AlertEvent;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Turns one alert event into one message per recipient per channel, and records the outcome.
 * A failure on one channel or one recipient never stops the others.
 */
@Service
public class AlertDispatchService {

    private static final Logger log = LoggerFactory.getLogger(AlertDispatchService.class);

    private final RecipientLookup recipientLookup;
    private final EmailSender emailSender;
    private final WhatsAppSender whatsAppSender;
    private final AlertMessageFactory messageFactory;
    private final AlertLogRepository repository;

    public AlertDispatchService(RecipientLookup recipientLookup, EmailSender emailSender,
                                WhatsAppSender whatsAppSender, AlertMessageFactory messageFactory,
                                AlertLogRepository repository) {
        this.recipientLookup = recipientLookup;
        this.emailSender = emailSender;
        this.whatsAppSender = whatsAppSender;
        this.messageFactory = messageFactory;
        this.repository = repository;
    }

    @Transactional
    public void dispatch(AlertEvent event) {
        List<AlertRecipient> recipients = recipientLookup.recipientsFor(event.hazard(), event.ward());
        if (recipients.isEmpty()) {
            log.warn("No alert recipients for {} in {}", event.hazard(), event.ward());
            return;
        }
        log.info("Dispatching {} alert for incident {} to {} recipient(s)",
                event.hazard(), event.incidentId(), recipients.size());

        for (AlertRecipient recipient : recipients) {
            sendEmail(event, recipient);
            sendWhatsApp(event, recipient);
        }
    }

    private void sendEmail(AlertEvent event, AlertRecipient recipient) {
        if (alreadySent(event, AlertChannel.EMAIL, recipient)) {
            return;
        }
        AlertLog entry = newEntry(event, AlertChannel.EMAIL, recipient, recipient.email());
        entry.setMessage(messageFactory.subject(event));

        if (!StringUtils.hasText(recipient.email())) {
            entry.setStatus(DeliveryStatus.SKIPPED);
            entry.setErrorMessage("No email address on file");
        } else if (!emailSender.isEnabled()) {
            entry.setStatus(DeliveryStatus.SKIPPED);
            entry.setErrorMessage("Email channel is disabled");
        } else {
            try {
                emailSender.send(recipient.email(), messageFactory.subject(event),
                        messageFactory.body(event, recipient.fullName()));
                entry.setStatus(DeliveryStatus.SENT);
            } catch (Exception ex) {
                entry.setStatus(DeliveryStatus.FAILED);
                entry.setErrorMessage(trim(ex.getMessage()));
                log.warn("Email alert to {} failed: {}", recipient.email(), ex.getMessage());
            }
        }
        repository.save(entry);
    }

    private void sendWhatsApp(AlertEvent event, AlertRecipient recipient) {
        if (alreadySent(event, AlertChannel.WHATSAPP, recipient)) {
            return;
        }
        AlertLog entry = newEntry(event, AlertChannel.WHATSAPP, recipient, recipient.phoneNumber());
        entry.setMessage(messageFactory.whatsAppText(event));

        if (!StringUtils.hasText(recipient.phoneNumber())) {
            entry.setStatus(DeliveryStatus.SKIPPED);
            entry.setErrorMessage("No phone number on file");
        } else if (!whatsAppSender.isEnabled()) {
            entry.setStatus(DeliveryStatus.SKIPPED);
            entry.setErrorMessage("WhatsApp channel is disabled");
        } else {
            try {
                whatsAppSender.send(recipient.phoneNumber(), messageFactory.whatsAppText(event));
                entry.setStatus(DeliveryStatus.SENT);
            } catch (Exception ex) {
                entry.setStatus(DeliveryStatus.FAILED);
                entry.setErrorMessage(trim(ex.getMessage()));
                log.warn("WhatsApp alert to {} failed: {}", recipient.phoneNumber(), ex.getMessage());
            }
        }
        repository.save(entry);
    }

    /** Guards against the same event being delivered twice if a message is redelivered. */
    private boolean alreadySent(AlertEvent event, AlertChannel channel, AlertRecipient recipient) {
        return repository.existsByEventIdAndChannelAndRecipientUsername(
                event.eventId(), channel, recipient.username());
    }

    private AlertLog newEntry(AlertEvent event, AlertChannel channel, AlertRecipient recipient, String address) {
        AlertLog entry = new AlertLog();
        entry.setEventId(event.eventId());
        entry.setHazard(event.hazard());
        entry.setIncidentId(event.incidentId());
        entry.setWard(event.ward());
        entry.setSeverity(event.severity());
        entry.setChannel(channel);
        entry.setRecipientUsername(recipient.username());
        entry.setRecipientAddress(address);
        entry.setReason(trimTo(event.reason(), 300));
        entry.setSentAt(LocalDateTime.now());
        return entry;
    }

    private static String trim(String value) {
        return trimTo(value, 500);
    }

    private static String trimTo(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
