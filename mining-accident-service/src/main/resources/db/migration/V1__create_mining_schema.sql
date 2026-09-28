-- mining-accident-service schema (database: dpdms_mining). Applied automatically by Flyway on startup.

CREATE TABLE mining_accident_incident (
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
    -- the five Mining Accident indicators
    mine_name                   VARCHAR(120) NOT NULL,
    mine_type                   VARCHAR(20)  NOT NULL,
    accident_type               VARCHAR(30)  NOT NULL,
    trapped_or_injured_miners   INT          NOT NULL,
    fatalities                  INT          NOT NULL,
    rescue_ongoing              BOOLEAN      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_mining_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CORRECTIONS_REQUESTED')),
    CONSTRAINT chk_mining_severity CHECK (severity IN ('LOW', 'MODERATE', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_mining_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_mining_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_mining_mine_type CHECK (mine_type IN ('FORMAL', 'ARTISANAL')),
    CONSTRAINT chk_mining_accident_type CHECK (accident_type IN ('COLLAPSE', 'GAS_EXPLOSION', 'FLOODING', 'FALL_OF_GROUND'))
);

CREATE INDEX idx_mining_status ON mining_accident_incident (status);
CREATE INDEX idx_mining_ward ON mining_accident_incident (ward);
CREATE INDEX idx_mining_occurred ON mining_accident_incident (occurred_at);

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
