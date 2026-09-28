package zw.ac.uz.dpdms.zoonotic.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.common.dto.ReviewRequest;
import zw.ac.uz.dpdms.common.workflow.WorkflowAction;
import zw.ac.uz.dpdms.zoonotic.dto.AuditEntryResponse;
import zw.ac.uz.dpdms.zoonotic.dto.ZoonoticDiseaseRequest;
import zw.ac.uz.dpdms.zoonotic.dto.ZoonoticDiseaseResponse;
import zw.ac.uz.dpdms.zoonotic.service.ZoonoticDiseaseIncidentService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/zoonotic-diseases")
@Tag(name = "Zoonotic Disease incidents", description = "Capture, approval workflow and reporting for zoonotic diseases")
public class ZoonoticDiseaseController {

    private final ZoonoticDiseaseIncidentService service;

    public ZoonoticDiseaseController(ZoonoticDiseaseIncidentService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Capture a zoonotic disease incident (zoonotic disease ward recorders only; starts as PENDING)")
    public ZoonoticDiseaseResponse create(@Valid @RequestBody ZoonoticDiseaseRequest request) {
        return service.create(request);
    }

    @GetMapping
    @Operation(summary = "List incidents visible to you, with optional filters")
    public List<ZoonoticDiseaseResponse> list(
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.list(ward, district, status, severity, from, to);
    }

    @GetMapping("/{id}")
    public ZoonoticDiseaseResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit your own record while it is PENDING or CORRECTIONS_REQUESTED")
    public ZoonoticDiseaseResponse update(@PathVariable Long id, @Valid @RequestBody ZoonoticDiseaseRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    // ---------- approval workflow ----------

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve (zoonotic disease supervisor only). Approved records reach the dashboard, map and reports.")
    public ZoonoticDiseaseResponse approve(@PathVariable Long id, @Valid @RequestBody(required = false) ReviewRequest request) {
        return service.review(id, WorkflowAction.APPROVE, comment(request));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject with a reason (zoonotic disease supervisor only)")
    public ZoonoticDiseaseResponse reject(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return service.review(id, WorkflowAction.REJECT, comment(request));
    }

    @PostMapping("/{id}/request-corrections")
    @Operation(summary = "Send back to the recorder with a reason (zoonotic disease supervisor only)")
    public ZoonoticDiseaseResponse requestCorrections(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return service.review(id, WorkflowAction.REQUEST_CORRECTIONS, comment(request));
    }

    @PostMapping("/{id}/resubmit")
    @Operation(summary = "Resubmit a corrected record for approval (the original recorder only)")
    public ZoonoticDiseaseResponse resubmit(@PathVariable Long id) {
        return service.resubmit(id);
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Audit trail: who acted, when, and what changed")
    public List<AuditEntryResponse> audit(@PathVariable Long id) {
        return service.auditTrail(id).stream().map(AuditEntryResponse::from).toList();
    }

    // ---------- used by dashboard-service and report-service ----------

    @GetMapping("/approved")
    @Operation(summary = "Approved incidents in the shared summary shape (for the dashboard, map and reports)")
    public List<IncidentSummary> approved(
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.approvedSummaries(ward, district, severity, from, to);
    }

    private static String comment(ReviewRequest request) {
        return request == null ? null : request.comment();
    }
}
