package zw.ac.uz.dpdms.dashboard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.dashboard.client.HazardServiceClient;
import zw.ac.uz.dpdms.dashboard.dto.DashboardSummary;
import zw.ac.uz.dpdms.dashboard.dto.MapPoint;
import zw.ac.uz.dpdms.dashboard.service.DashboardService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private final HazardServiceClient client = Mockito.mock(HazardServiceClient.class);
    private final DashboardService service = new DashboardService(client);

    private IncidentSummary incident(HazardType hazard, String ward, Severity severity,
                                     LocalDateTime when, Double lat, Double lon) {
        return new IncidentSummary(hazard, 1L, ward, "Rushinga", "Mashonaland Central", when, severity,
                IncidentStatus.APPROVED, lat, lon, "recorder", hazard.getLabel() + " in " + ward, Map.of());
    }

    private void given(List<IncidentSummary> incidents) {
        when(client.approvedIncidents(isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(incidents);
        when(client.approvedIncidents(any(), any(), any(), any(), any())).thenReturn(incidents);
    }

    @Test
    @DisplayName("Counts by hazard, severity and ward, with every hazard listed even at zero")
    void countsAreAggregated() {
        LocalDateTime now = LocalDateTime.now().minusDays(3);
        given(List.of(
                incident(HazardType.FLOOD, "Ward 1", Severity.HIGH, now, -16.6, 32.0),
                incident(HazardType.FLOOD, "Ward 2", Severity.LOW, now, -16.5, 32.1),
                incident(HazardType.FIRE, "Ward 1", Severity.HIGH, now, -16.4, 32.2)));

        DashboardSummary summary = service.summary(null, null, null, null, null);

        assertEquals(3, summary.totalIncidents());
        assertEquals(2L, summary.byHazard().get("Flood"));
        assertEquals(1L, summary.byHazard().get("Fire"));
        assertEquals(0L, summary.byHazard().get("Drought"));
        assertEquals(2L, summary.bySeverity().get("HIGH"));
        assertEquals(2L, summary.byWard().get("Ward 1"));
        assertEquals(5, summary.byHazard().size(), "all five hazards are always present");
    }

    @Test
    void recentIncidentsAreNewestFirst() {
        given(List.of(
                incident(HazardType.FLOOD, "Ward 1", Severity.LOW, LocalDateTime.now().minusDays(10), -16.6, 32.0),
                incident(HazardType.FIRE, "Ward 2", Severity.LOW, LocalDateTime.now().minusDays(1), -16.5, 32.1)));

        DashboardSummary summary = service.summary(null, null, null, null, null);
        assertEquals(HazardType.FIRE, summary.recent().get(0).hazard());
    }

    @Test
    void trendGroupsByMonth() {
        LocalDateTime thisMonth = LocalDateTime.now().withDayOfMonth(1).plusDays(2);
        given(List.of(
                incident(HazardType.FLOOD, "Ward 1", Severity.LOW, thisMonth, -16.6, 32.0),
                incident(HazardType.FLOOD, "Ward 1", Severity.LOW, thisMonth.plusDays(1), -16.6, 32.0)));

        assertEquals(1, service.summary(null, null, null, null, null).trend().size());
        assertEquals(2L, service.summary(null, null, null, null, null).trend().get(0).total());
    }

    @Test
    @DisplayName("Map points need coordinates; incidents without them are left off the map")
    void mapPointsRequireCoordinates() {
        given(List.of(
                incident(HazardType.FLOOD, "Ward 1", Severity.LOW, LocalDateTime.now(), -16.6, 32.0),
                incident(HazardType.FIRE, "Ward 2", Severity.LOW, LocalDateTime.now(), null, null)));

        List<MapPoint> points = service.mapPoints(null, null, null, null, null);
        assertEquals(1, points.size());
        assertEquals("Flood", points.get(0).hazardLabel());
        assertTrue(points.get(0).latitude() < 0);
    }
}
