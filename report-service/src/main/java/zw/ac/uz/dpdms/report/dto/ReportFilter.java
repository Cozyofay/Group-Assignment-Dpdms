package zw.ac.uz.dpdms.report.dto;

import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Severity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reports are filterable by hazard type, ward, district, date range and severity, as the brief
 * requires. Approval status needs no filter: only APPROVED records are ever returned.
 */
public record ReportFilter(List<HazardType> hazards, String ward, String district,
                           Severity severity, LocalDateTime from, LocalDateTime to) {

    public List<HazardType> hazardsOrAll() {
        return hazards == null || hazards.isEmpty() ? List.of(HazardType.values()) : hazards;
    }

    public String describe() {
        StringBuilder text = new StringBuilder();
        text.append("Hazards: ").append(hazardsOrAll().stream().map(HazardType::getLabel).toList());
        if (ward != null && !ward.isBlank()) {
            text.append("  |  Ward: ").append(ward);
        }
        if (district != null && !district.isBlank()) {
            text.append("  |  District: ").append(district);
        }
        if (severity != null) {
            text.append("  |  Severity: ").append(severity);
        }
        if (from != null || to != null) {
            text.append("  |  Period: ").append(from == null ? "start" : from.toLocalDate())
                    .append(" to ").append(to == null ? "today" : to.toLocalDate());
        }
        return text.toString();
    }
}
