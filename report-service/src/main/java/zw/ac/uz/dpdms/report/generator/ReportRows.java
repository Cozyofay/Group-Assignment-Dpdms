package zw.ac.uz.dpdms.report.generator;

import zw.ac.uz.dpdms.common.dto.IncidentSummary;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Flattens incidents of any hazard into the same table shape, used by all four formats. */
public final class ReportRows {

    public static final List<String> HEADERS = List.of(
            "Hazard", "Ref", "Occurred", "Ward", "District", "Province",
            "Severity", "Status", "Latitude", "Longitude", "Reported by", "Summary", "Indicators");

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ReportRows() {
    }

    public static List<String> toRow(IncidentSummary incident) {
        List<String> row = new ArrayList<>();
        row.add(incident.hazard().getLabel());
        row.add(String.valueOf(incident.id()));
        row.add(incident.occurredAt() == null ? "" : incident.occurredAt().format(WHEN));
        row.add(nullSafe(incident.ward()));
        row.add(nullSafe(incident.district()));
        row.add(nullSafe(incident.province()));
        row.add(String.valueOf(incident.severity()));
        row.add(String.valueOf(incident.status()));
        row.add(String.valueOf(incident.latitude()));
        row.add(String.valueOf(incident.longitude()));
        row.add(nullSafe(incident.reporterUsername()));
        row.add(nullSafe(incident.headline()));
        row.add(indicators(incident));
        return row;
    }

    public static String indicators(IncidentSummary incident) {
        Map<String, Object> values = incident.indicators();
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.entrySet().stream()
                .map(entry -> readable(entry.getKey()) + ": " + entry.getValue())
                .collect(Collectors.joining("; "));
    }

    /** "peakWaterLevelMetres" -> "Peak water level metres" */
    public static String readable(String camelCase) {
        String spaced = camelCase.replaceAll("([A-Z])", " $1").trim().toLowerCase();
        return spaced.substring(0, 1).toUpperCase() + spaced.substring(1);
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
