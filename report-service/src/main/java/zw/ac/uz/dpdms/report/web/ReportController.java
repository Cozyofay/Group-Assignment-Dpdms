package zw.ac.uz.dpdms.report.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.report.dto.GeneratedReport;
import zw.ac.uz.dpdms.report.dto.ReportFilter;
import zw.ac.uz.dpdms.report.dto.ReportFormat;
import zw.ac.uz.dpdms.report.service.ReportService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reports are downloaded with GET so that national (read-only) users can generate them too.
 * Every endpoint returns approved records only.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "PDF, Word, Excel and CSV reports of approved incidents")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{format}")
    @Operation(summary = "Download a report: format is pdf, docx, xlsx or csv")
    public ResponseEntity<byte[]> download(
            @PathVariable ReportFormat format,
            @RequestParam(required = false) List<HazardType> hazards,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        GeneratedReport report = reportService.generate(format,
                new ReportFilter(hazards, ward, district, severity, from, to));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, report.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.fileName() + "\"")
                .body(report.content());
    }

    @GetMapping("/preview")
    @Operation(summary = "The incidents a report would contain, as JSON (used by the front end)")
    public List<IncidentSummary> preview(
            @RequestParam(required = false) List<HazardType> hazards,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return reportService.preview(new ReportFilter(hazards, ward, district, severity, from, to));
    }
}
