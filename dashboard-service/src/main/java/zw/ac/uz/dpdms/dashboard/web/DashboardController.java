package zw.ac.uz.dpdms.dashboard.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.dashboard.dto.DashboardSummary;
import zw.ac.uz.dpdms.dashboard.dto.MapPoint;
import zw.ac.uz.dpdms.dashboard.service.DashboardService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Live counts, trends, recent incidents and map markers")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Counts by hazard, severity and ward, the 12-month trend and recent incidents")
    public DashboardSummary summary(
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return dashboardService.summary(ward, district, severity, from, to);
    }

    @GetMapping("/map")
    @Operation(summary = "Approved incidents as map markers, with their GPS coordinates and details")
    public List<MapPoint> map(
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return dashboardService.mapPoints(ward, district, severity, from, to);
    }
}
