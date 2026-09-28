package zw.ac.uz.dpdms.common.dto;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Hazard-neutral view of an incident. Every hazard service returns this shape from its
 * "approved" endpoint so the dashboard, map and report-service can aggregate all five uniformly.
 */
public record IncidentSummary(
        HazardType hazard,
        Long id,
        String ward,
        String district,
        String province,
        LocalDateTime occurredAt,
        Severity severity,
        IncidentStatus status,
        Double latitude,
        Double longitude,
        String reporterUsername,
        String headline,
        Map<String, Object> indicators) {
}
