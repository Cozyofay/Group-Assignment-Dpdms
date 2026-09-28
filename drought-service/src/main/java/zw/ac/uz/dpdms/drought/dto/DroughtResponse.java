package zw.ac.uz.dpdms.drought.dto;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.drought.domain.DroughtIncident;


import java.time.LocalDateTime;

public record DroughtResponse(
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
        Double rainfallDeficitMm,
        Integer consecutiveDryDays,
        Double cropFailurePercentage,
        Integer peopleFacingWaterShortages,
        Integer livestockMortalityCount,
        String reviewComment,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static DroughtResponse from(DroughtIncident incident) {
        return new DroughtResponse(
                incident.getId(), HazardType.DROUGHT, incident.getWard(), incident.getDistrict(),
                incident.getProvince(), incident.getOccurredAt(), incident.getReporterUsername(),
                incident.getSeverity(), incident.getStatus(), incident.getLatitude(), incident.getLongitude(),
                incident.getRainfallDeficitMm(),
                incident.getConsecutiveDryDays(),
                incident.getCropFailurePercentage(),
                incident.getPeopleFacingWaterShortages(),
                incident.getLivestockMortalityCount(),
                incident.getReviewComment(), incident.getReviewedBy(), incident.getReviewedAt(),
                incident.getCreatedAt(), incident.getUpdatedAt());
    }
}
