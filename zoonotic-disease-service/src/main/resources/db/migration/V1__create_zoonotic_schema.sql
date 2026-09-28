-- zoonotic-disease-service schema (database: dpdms_zoonotic). Applied automatically by Flyway on startup.

CREATE TABLE zoonotic_disease_incident (
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
    -- the five Zoonotic Disease indicators
    pathogen_name             VARCHAR(120) NOT NULL,
    animal_species_affected   VARCHAR(120) NOT NULL,
    confirmed_human_cases     INT          NOT NULL,
    confirmed_animal_cases    INT          NOT NULL,
    classification            VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_zoonotic_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CORRECTIONS_REQUESTED')),
    CONSTRAINT chk_zoonotic_severity CHECK (severity IN ('LOW', 'MODERATE', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_zoonotic_latitude CHECK (latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_zoonotic_longitude CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_zoonotic_classification CHECK (classification IN ('CLUSTER', 'OUTBREAK'))
);

CREATE INDEX idx_zoonotic_status ON zoonotic_disease_incident (status);
CREATE INDEX idx_zoonotic_ward ON zoonotic_disease_incident (ward);
CREATE INDEX idx_zoonotic_occurred ON zoonotic_disease_incident (occurred_at);

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
