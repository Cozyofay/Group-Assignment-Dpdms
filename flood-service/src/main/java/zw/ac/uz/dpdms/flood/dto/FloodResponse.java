package zw.ac.uz.dpdms.flood.dto;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.flood.domain.FloodIncident;

import java.time.LocalDateTime;

public record FloodResponse(
        Long id,
        HazardType hazard,
        String ward,
        String district,
        String province,
        LocalDateTime occurredAt,
        String reporterUsername,
        Severity severity,
        IncidentStatus status,
        Double latitude,
        Double longitude,
        Double peakWaterLevelMetres,
        String riverBasin,
        Integer householdsDisplaced,
        Double areaFloodedHectares,
        Integer inundationDurationDays,
        String reviewComment,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static FloodResponse from(FloodIncident incident) {
        return new FloodResponse(
                incident.getId(), HazardType.FLOOD, incident.getWard(), incident.getDistrict(),
                incident.getProvince(), incident.getOccurredAt(), incident.getReporterUsername(),
                incident.getSeverity(), incident.getStatus(), incident.getLatitude(), incident.getLongitude(),
                incident.getPeakWaterLevelMetres(), incident.getRiverBasin(), incident.getHouseholdsDisplaced(),
                incident.getAreaFloodedHectares(), incident.getInundationDurationDays(),
                incident.getReviewComment(), incident.getReviewedBy(), incident.getReviewedAt(),
                incident.getCreatedAt(), incident.getUpdatedAt());
    }
}
