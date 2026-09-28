package zw.ac.uz.dpdms.dashboard.service;

import org.springframework.stereotype.Service;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.dashboard.client.HazardServiceClient;
import zw.ac.uz.dpdms.dashboard.dto.DashboardSummary;
import zw.ac.uz.dpdms.dashboard.dto.MapPoint;
import zw.ac.uz.dpdms.dashboard.dto.TrendPoint;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Builds the live view of what is happening: counts, trends, recent incidents and map markers. */
@Service
public class DashboardService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int RECENT_LIMIT = 10;
    private static final int TREND_MONTHS = 12;

    private final HazardServiceClient client;

    public DashboardService(HazardServiceClient client) {
        this.client = client;
    }

    public DashboardSummary summary(String ward, String district, Severity severity,
                                    LocalDateTime from, LocalDateTime to) {
        List<IncidentSummary> incidents = client.approvedIncidents(ward, district, severity, from, to);

        Map<String, Long> byHazard = new LinkedHashMap<>();
        for (HazardType hazard : HazardType.values()) {
            byHazard.put(hazard.getLabel(), 0L);
        }
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Severity value : Severity.values()) {
            bySeverity.put(value.name(), 0L);
        }
        Map<String, Long> byWard = new TreeMap<>();

        for (IncidentSummary incident : incidents) {
            byHazard.merge(incident.hazard().getLabel(), 1L, Long::sum);
            bySeverity.merge(String.valueOf(incident.severity()), 1L, Long::sum);
            byWard.merge(incident.ward() == null ? "Unknown" : incident.ward(), 1L, Long::sum);
        }

        List<IncidentSummary> recent = incidents.stream()
                .sorted(Comparator.comparing(IncidentSummary::occurredAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(RECENT_LIMIT)
                .toList();

        return new DashboardSummary(incidents.size(), byHazard, bySeverity, byWard, trend(incidents), recent);
    }

    /** Incidents per month for the last 12 months, split by hazard, oldest first. */
    private List<TrendPoint> trend(List<IncidentSummary> incidents) {
        Map<String, Map<String, Long>> byMonth = new TreeMap<>();
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(TREND_MONTHS);

        for (IncidentSummary incident : incidents) {
            if (incident.occurredAt() == null || incident.occurredAt().isBefore(cutoff)) {
                continue;
            }
            String month = incident.occurredAt().format(MONTH);
            byMonth.computeIfAbsent(month, key -> new LinkedHashMap<>())
                    .merge(incident.hazard().getLabel(), 1L, Long::sum);
        }

        List<TrendPoint> points = new ArrayList<>();
        byMonth.forEach((month, counts) -> {
            long total = counts.values().stream().mapToLong(Long::longValue).sum();
            points.add(new TrendPoint(month, total, counts));
        });
        return points;
    }

    /** Only approved incidents are plotted, and each carries its GPS coordinates and details. */
    public List<MapPoint> mapPoints(String ward, String district, Severity severity,
                                    LocalDateTime from, LocalDateTime to) {
        return client.approvedIncidents(ward, district, severity, from, to).stream()
                .filter(incident -> incident.latitude() != null && incident.longitude() != null)
                .map(MapPoint::from)
                .toList();
    }
}
