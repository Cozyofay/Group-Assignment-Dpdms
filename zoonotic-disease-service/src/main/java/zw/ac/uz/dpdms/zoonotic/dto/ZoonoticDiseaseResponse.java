package zw.ac.uz.dpdms.zoonotic.dto;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.zoonotic.domain.ZoonoticDiseaseIncident;
import zw.ac.uz.dpdms.zoonotic.domain.EventClassification;

import java.time.LocalDateTime;

public record ZoonoticDiseaseResponse(
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
        String pathogenName,
        String animalSpeciesAffected,
        Integer confirmedHumanCases,
        Integer confirmedAnimalCases,
        EventClassification classification,
        String reviewComment,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ZoonoticDiseaseResponse from(ZoonoticDiseaseIncident incident) {
        return new ZoonoticDiseaseResponse(
                incident.getId(), HazardType.ZOONOTIC_DISEASE, incident.getWard(), incident.getDistrict(),
                incident.getProvince(), incident.getOccurredAt(), incident.getReporterUsername(),
                incident.getSeverity(), incident.getStatus(), incident.getLatitude(), incident.getLongitude(),
                incident.getPathogenName(),
                incident.getAnimalSpeciesAffected(),
                incident.getConfirmedHumanCases(),
                incident.getConfirmedAnimalCases(),
                incident.getClassification(),
                incident.getReviewComment(), incident.getReviewedBy(), incident.getReviewedAt(),
                incident.getCreatedAt(), incident.getUpdatedAt());
    }
}
