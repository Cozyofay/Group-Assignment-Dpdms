package zw.ac.uz.dpdms.dashboard.dto;

import zw.ac.uz.dpdms.common.dto.IncidentSummary;

import java.util.List;
import java.util.Map;

/**
 * Everything the dashboard shows: totals, counts by hazard, severity, ward and month,
 * plus the most recent incidents.
 */
public record DashboardSummary(
        long totalIncidents,
        Map<String, Long> byHazard,
        Map<String, Long> bySeverity,
        Map<String, Long> byWard,
        List<TrendPoint> trend,
        List<IncidentSummary> recent) {
}
