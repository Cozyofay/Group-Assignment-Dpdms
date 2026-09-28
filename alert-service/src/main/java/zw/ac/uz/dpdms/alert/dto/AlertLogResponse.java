package zw.ac.uz.dpdms.alert.dto;

import zw.ac.uz.dpdms.alert.domain.AlertChannel;
import zw.ac.uz.dpdms.alert.domain.AlertLog;
import zw.ac.uz.dpdms.alert.domain.DeliveryStatus;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Severity;

import java.time.LocalDateTime;

public record AlertLogResponse(Long id, String eventId, HazardType hazard, Long incidentId, String ward,
                               Severity severity, AlertChannel channel, String recipientUsername,
                               String recipientAddress, String reason, DeliveryStatus status,
                               String errorMessage, LocalDateTime sentAt) {

    public static AlertLogResponse from(AlertLog entry) {
        return new AlertLogResponse(entry.getId(), entry.getEventId(), entry.getHazard(), entry.getIncidentId(),
                entry.getWard(), entry.getSeverity(), entry.getChannel(), entry.getRecipientUsername(),
                entry.getRecipientAddress(), entry.getReason(), entry.getStatus(), entry.getErrorMessage(),
                entry.getSentAt());
    }
}
