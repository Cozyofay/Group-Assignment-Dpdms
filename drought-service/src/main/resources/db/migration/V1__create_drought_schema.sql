-- drought-service schema (database: dpdms_drought). Applied automatically by Flyway on startup.

CREATE TABLE drought_incident (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    -- shared incident metadata (identical in all five hazard services)
    ward              VARCHAR(100) NOT NULL,
    district          VARCHAR(100) NOT NULL,
    province          VARCHAR(100) NOT NULL,
    occurred_at       DATETIME     NOT NULL,
    reporter_username VARCHAR(100) NOT NULL,
    severity          VARCHAR(20)  NOT NULL,
    status            VARCHAR(30)  NOT NULL,
    latitude          DOUBLE       NOT NULL,
    longitude         DOUBLE       NOT NULL,
    review_comment    VARCHAR(1000),
    reviewed_by       VARCHAR(100),
    reviewed_at       DATETIME,
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME,
    version           BIGINT,
    -- the five Drought indicators
    rainfall_deficit_mm             DOUBLE       NOT NULL,
    consecutive_dry_days            INT          NOT NULL,
    crop_failure_percentage         DOUBLE       NOT NULL,
    people_facing_water_shortages   INT          NOT NULL,
    livestock_mortality_count       INT          NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_drought_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CORRECTIONS_REQUESTED')),
    CONSTRAINT chk_drought_severity CHECK (severity IN ('LOW', 'MODERATE', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_drought_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_drought_longitude CHECK (longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_drought_status ON drought_incident (status);
CREATE INDEX idx_drought_ward ON drought_incident (ward);
CREATE INDEX idx_drought_occurred ON drought_incident (occurred_at);

-- audit trail: every state transition, who acted, when, and what changed
CREATE TABLE incident_audit_log (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    hazard          VARCHAR(30)  NOT NULL,
    incident_id     BIGINT       NOT NULL,
    action          VARCHAR(30)  NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30),
    actor_username  VARCHAR(100) NOT NULL,
    actor_role      VARCHAR(30)  NOT NULL,
    acted_at        DATETIME     NOT NULL,
    changes         TEXT,
    comment         VARCHAR(1000),
    PRIMARY KEY (id)
);

CREATE INDEX idx_audit_incident ON incident_audit_log (incident_id);
