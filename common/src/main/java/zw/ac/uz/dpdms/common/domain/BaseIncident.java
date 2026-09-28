package zw.ac.uz.dpdms.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import zw.ac.uz.dpdms.common.dto.BaseIncidentRequest;
import zw.ac.uz.dpdms.common.dto.IncidentSummary;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Shared incident metadata inherited by every hazard entity (FloodIncident, DroughtIncident, ...).
 * Keeping it in one class guarantees the five services store the metadata identically, so the
 * dashboard, map and reports can aggregate them uniformly.
 */
@MappedSuperclass
public abstract class BaseIncident implements ScopedIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String ward;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "reporter_username", nullable = false, length = 100)
    private String reporterUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private IncidentStatus status = IncidentStatus.PENDING;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Optimistic locking: stops two users silently overwriting each other's edits. */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = IncidentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** Copies the shared metadata fields from a validated request. */
    public void applyMetadata(BaseIncidentRequest request) {
        this.ward = request.getWard().trim();
        this.district = request.getDistrict().trim();
        this.province = request.getProvince().trim();
        this.occurredAt = request.getOccurredAt();
        this.severity = request.getSeverity();
        this.latitude = request.getLatitude();
        this.longitude = request.getLongitude();
    }

    /** One-line description used on map pop-ups, alerts and reports, e.g. "Flood - Mazowe basin, 2.4 m". */
    public abstract String headline();

    /** The hazard-specific indicators, in display order, for reports and the map pop-up. */
    public abstract Map<String, Object> indicators();

    public IncidentSummary toSummary(HazardType hazard) {
        return new IncidentSummary(hazard, id, ward, district, province, occurredAt, severity, status,
                latitude, longitude, reporterUsername, headline(), indicators());
    }

    // ---------- getters / setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public LocalDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(LocalDateTime occurredAt) { this.occurredAt = occurredAt; }

    @Override
    public String getReporterUsername() { return reporterUsername; }
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    @Override
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Long getVersion() { return version; }
}
