package zw.ac.uz.dpdms.dashboard.dto;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;

import java.time.LocalDateTime;
import java.util.Map;

/** A marker on the map. Clicking it reveals these details. */
public record MapPoint(Long id, HazardType hazard, String hazardLabel, Double latitude, Double longitude,
                       String ward, String district, Severity severity, LocalDateTime occurredAt,
                       String headline, String reporterUsername, Map<String, Object> indicators) {

    public static MapPoint from(IncidentSummary incident) {
        return new MapPoint(incident.id(), incident.hazard(), incident.hazard().getLabel(),
                incident.latitude(), incident.longitude(), incident.ward(), incident.district(),
                incident.severity(), incident.occurredAt(), incident.headline(),
                incident.reporterUsername(), incident.indicators());
    }
}
