package zw.ac.uz.dpdms.mining.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.ac.uz.dpdms.common.audit.AuditAction;
import zw.ac.uz.dpdms.common.audit.AuditService;
import zw.ac.uz.dpdms.common.audit.IncidentAuditLog;
import zw.ac.uz.dpdms.common.domain.HazardType;
import zw.ac.uz.dpdms.common.domain.IncidentStatus;
import zw.ac.uz.dpdms.common.domain.Severity;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;
import zw.ac.uz.dpdms.common.exception.ResourceNotFoundException;
import zw.ac.uz.dpdms.common.messaging.AlertEvent;
import zw.ac.uz.dpdms.common.security.AuthenticatedUser;
import zw.ac.uz.dpdms.common.security.CurrentUser;
import zw.ac.uz.dpdms.common.security.HazardAccessPolicy;
import zw.ac.uz.dpdms.common.util.ChangeDiff;
import zw.ac.uz.dpdms.common.workflow.ApprovalWorkflow;
import zw.ac.uz.dpdms.common.workflow.WorkflowAction;
import zw.ac.uz.dpdms.mining.alert.AlertPublisher;
import zw.ac.uz.dpdms.mining.alert.MiningAccidentAlertRules;
import zw.ac.uz.dpdms.mining.domain.MiningAccidentIncident;
import zw.ac.uz.dpdms.mining.domain.MiningAccidentIncidentRepository;
import zw.ac.uz.dpdms.mining.domain.MiningAccidentSpecifications;
import zw.ac.uz.dpdms.mining.dto.MiningAccidentRequest;
import zw.ac.uz.dpdms.mining.dto.MiningAccidentResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * All Mining Accident business rules in one place. Every method starts by checking the caller's authority
 * with HazardAccessPolicy - this is the backend enforcement the brief insists on.
 */
@Service
@Transactional
public class MiningAccidentIncidentService {

    private static final HazardType HAZARD = HazardType.MINING_ACCIDENT;
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "occurredAt");

    private final MiningAccidentIncidentRepository repository;
    private final AuditService auditService;
    private final AlertPublisher alertPublisher;
    private final ObjectMapper objectMapper;

    public MiningAccidentIncidentService(MiningAccidentIncidentRepository repository, AuditService auditService,
                                     AlertPublisher alertPublisher, ObjectMapper objectMapper) {
        this.repository = repository;
        this.auditService = auditService;
        this.alertPublisher = alertPublisher;
        this.objectMapper = objectMapper;
    }

    // ---------------- create / read / update / delete ----------------

    public MiningAccidentResponse create(MiningAccidentRequest request) {
        AuthenticatedUser user = CurrentUser.get();
        HazardAccessPolicy.require(HazardAccessPolicy.canCreate(user, HAZARD, request.getWard()),
                "You may only capture Mining Accident records for " + user.ward());

        MiningAccidentIncident incident = new MiningAccidentIncident();
        incident.applyMetadata(request);
        incident.applyIndicators(request);
        incident.setReporterUsername(user.username());
        incident.setStatus(IncidentStatus.PENDING);
        MiningAccidentIncident saved = repository.save(incident);

        auditService.record(HAZARD, saved.getId(), AuditAction.CREATED, null, IncidentStatus.PENDING,
                user, "Captured: " + saved.headline(), null);
        raiseAlertIfNeeded(saved);
        return MiningAccidentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<MiningAccidentResponse> list(String ward, String district, IncidentStatus status, Severity severity,
                                         LocalDateTime from, LocalDateTime to) {
        AuthenticatedUser user = CurrentUser.get();
        Specification<MiningAccidentIncident> spec = Specification
                .where(MiningAccidentSpecifications.visibleTo(user))
                .and(MiningAccidentSpecifications.filters(ward, district, status, severity, from, to));
        return repository.findAll(spec, NEWEST_FIRST).stream().map(MiningAccidentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MiningAccidentResponse get(Long id) {
        AuthenticatedUser user = CurrentUser.get();
        MiningAccidentIncident incident = find(id);
        HazardAccessPolicy.require(HazardAccessPolicy.canView(user, HAZARD, incident),
                "You are not allowed to view this record");
        return MiningAccidentResponse.from(incident);
    }

    public MiningAccidentResponse update(Long id, MiningAccidentRequest request) {
        AuthenticatedUser user = CurrentUser.get();
        MiningAccidentIncident incident = find(id);
        HazardAccessPolicy.require(HazardAccessPolicy.canEdit(user, HAZARD, incident),
                "Only the recorder who captured this record may edit it, and only before it is approved or rejected");
        HazardAccessPolicy.require(HazardAccessPolicy.canCreate(user, HAZARD, request.getWard()),
                "You cannot move this record to another ward");

        boolean alertedBefore = MiningAccidentAlertRules.alertReason(incident).isPresent();
        Map<String, Object> before = ChangeDiff.snapshot(objectMapper, MiningAccidentResponse.from(incident));

        incident.applyMetadata(request);
        incident.applyIndicators(request);
        MiningAccidentIncident saved = repository.save(incident);

        Map<String, Object> after = ChangeDiff.snapshot(objectMapper, MiningAccidentResponse.from(saved));
        auditService.record(HAZARD, saved.getId(), AuditAction.UPDATED, saved.getStatus(), saved.getStatus(),
                user, ChangeDiff.describe(before, after), null);

        if (!alertedBefore) {
            raiseAlertIfNeeded(saved);
        }
        return MiningAccidentResponse.from(saved);
    }

    public void delete(Long id) {
        AuthenticatedUser user = CurrentUser.get();
        MiningAccidentIncident incident = find(id);
        HazardAccessPolicy.require(HazardAccessPolicy.canDelete(user, HAZARD, incident),
                "You are not allowed to delete this record");
        auditService.record(HAZARD, incident.getId(), AuditAction.DELETED, incident.getStatus(), null,
                user, "Deleted: " + incident.headline(), null);
        repository.delete(incident);
    }

    // ---------------- approval workflow ----------------

    /** Supervisor decision: APPROVE, REJECT or REQUEST_CORRECTIONS. */
    public MiningAccidentResponse review(Long id, WorkflowAction action, String comment) {
        AuthenticatedUser user = CurrentUser.get();
        HazardAccessPolicy.require(HazardAccessPolicy.canReview(user, HAZARD),
                "Only the provincial mining accident supervisor may "
                        + action.name().toLowerCase().replace('_', ' ') + " mining accident records");
        ApprovalWorkflow.validateComment(action, comment);

        MiningAccidentIncident incident = find(id);
        IncidentStatus from = incident.getStatus();
        IncidentStatus to = ApprovalWorkflow.apply(from, action);

        incident.setStatus(to);
        incident.setReviewComment(comment);
        incident.setReviewedBy(user.username());
        incident.setReviewedAt(LocalDateTime.now());
        MiningAccidentIncident saved = repository.save(incident);

        auditService.record(HAZARD, saved.getId(), AuditAction.from(action), from, to, user, null, comment);
        return MiningAccidentResponse.from(saved);
    }

    /** Recorder sends a corrected record back to the supervisor. */
    public MiningAccidentResponse resubmit(Long id) {
        AuthenticatedUser user = CurrentUser.get();
        MiningAccidentIncident incident = find(id);
        HazardAccessPolicy.require(HazardAccessPolicy.canResubmit(user, HAZARD, incident),
                "Only the recorder who captured this record may resubmit it, after corrections were requested");

        IncidentStatus from = incident.getStatus();
        IncidentStatus to = ApprovalWorkflow.apply(from, WorkflowAction.RESUBMIT);
        incident.setStatus(to);
        MiningAccidentIncident saved = repository.save(incident);

        auditService.record(HAZARD, saved.getId(), AuditAction.RESUBMITTED, from, to, user,
                "Resubmitted after corrections", null);
        return MiningAccidentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<IncidentAuditLog> auditTrail(Long id) {
        AuthenticatedUser user = CurrentUser.get();
        MiningAccidentIncident incident = find(id);
        HazardAccessPolicy.require(HazardAccessPolicy.canView(user, HAZARD, incident),
                "You are not allowed to view this record");
        return auditService.history(id);
    }

    // ---------------- cross-service reads ----------------

    /**
     * Approved records only, in the hazard-neutral shape the dashboard, map and report-service use.
     * Because it is filtered to APPROVED here, pending data can never reach a report or the map.
     */
    @Transactional(readOnly = true)
    public List<IncidentSummary> approvedSummaries(String ward, String district, Severity severity,
                                                   LocalDateTime from, LocalDateTime to) {
        Specification<MiningAccidentIncident> spec = Specification
                .where(MiningAccidentSpecifications.approvedOnly())
                .and(MiningAccidentSpecifications.filters(ward, district, null, severity, from, to));
        return repository.findAll(spec, NEWEST_FIRST).stream()
                .map(incident -> incident.toSummary(HAZARD))
                .toList();
    }

    // ---------------- helpers ----------------

    private MiningAccidentIncident find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mining Accident incident " + id + " not found"));
    }

    private void raiseAlertIfNeeded(MiningAccidentIncident incident) {
        Optional<String> reason = MiningAccidentAlertRules.alertReason(incident);
        reason.ifPresent(text -> alertPublisher.publish(AlertEvent.of(incident.toSummary(HAZARD), text)));
    }
}
