package zw.ac.uz.dpdms.mining.dto;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.mining.domain.MiningAccidentIncident;
import zw.ac.uz.dpdms.mining.domain.MineType;
import zw.ac.uz.dpdms.mining.domain.AccidentType;

import java.time.LocalDateTime;

public record MiningAccidentResponse(
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
        String mineName,
        MineType mineType,
        AccidentType accidentType,
        Integer trappedOrInjuredMiners,
        Integer fatalities,
        Boolean rescueOngoing,
        String reviewComment,
        String reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static MiningAccidentResponse from(MiningAccidentIncident incident) {
        return new MiningAccidentResponse(
                incident.getId(), HazardType.MINING_ACCIDENT, incident.getWard(), incident.getDistrict(),
                incident.getProvince(), incident.getOccurredAt(), incident.getReporterUsername(),
                incident.getSeverity(), incident.getStatus(), incident.getLatitude(), incident.getLongitude(),
                incident.getMineName(),
                incident.getMineType(),
                incident.getAccidentType(),
                incident.getTrappedOrInjuredMiners(),
                incident.getFatalities(),
                incident.getRescueOngoing(),
                incident.getReviewComment(), incident.getReviewedBy(), incident.getReviewedAt(),
                incident.getCreatedAt(), incident.getUpdatedAt());
    }
}
