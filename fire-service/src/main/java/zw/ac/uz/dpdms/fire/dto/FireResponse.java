package zw.ac.uz.dpdms.fire.dto;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.fire.domain.FireIncident;
import zw.ac.uz.dpdms.fire.domain.FireCause;

import java.time.LocalDateTime;

public record FireResponse(
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
        Double areaBurnedHectares,
        FireCause suspectedCause,
        Integer injuriesOrFatalities,
        Integer structuresDestroyed,
        Boolean stillActive,
        String reviewComment,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static FireResponse from(FireIncident incident) {
        return new FireResponse(
                incident.getId(), HazardType.FIRE, incident.getWard(), incident.getDistrict(),
                incident.getProvince(), incident.getOccurredAt(), incident.getReporterUsername(),
                incident.getSeverity(), incident.getStatus(), incident.getLatitude(), incident.getLongitude(),
                incident.getAreaBurnedHectares(),
                incident.getSuspectedCause(),
                incident.getInjuriesOrFatalities(),
                incident.getStructuresDestroyed(),
                incident.getStillActive(),
                incident.getReviewComment(), incident.getReviewedBy(), incident.getReviewedAt(),
                incident.getCreatedAt(), incident.getUpdatedAt());
    }
}
