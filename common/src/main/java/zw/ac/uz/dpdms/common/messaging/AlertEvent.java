package zw.ac.uz.dpdms.common.messaging;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;

import java.time.LocalDateTime;
import java.util.UUID;

/** Message published to RabbitMQ when an incident meets its hazard's alerting criteria. */
public record AlertEvent(
        String eventId,
        HazardType hazard,
        Long incidentId,
        String ward,
        String district,
        String province,
        Severity severity,
        Double latitude,
        Double longitude,
        LocalDateTime occurredAt,
        String headline,
        String reason,
        LocalDateTime raisedAt) {

    public static AlertEvent of(IncidentSummary incident, String reason) {
        return new AlertEvent(UUID.randomUUID().toString(), incident.hazard(), incident.id(),
                incident.ward(), incident.district(), incident.province(), incident.severity(),
                incident.latitude(), incident.longitude(), incident.occurredAt(), incident.headline(),
                reason, LocalDateTime.now());
    }
}
