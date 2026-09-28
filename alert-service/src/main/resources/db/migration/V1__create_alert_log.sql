-- alert-service schema (database: dpdms_alert). Applied automatically by Flyway on startup.
CREATE TABLE alert_log (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    event_id           VARCHAR(60)  NOT NULL,
    hazard             VARCHAR(30)  NOT NULL,
    incident_id        BIGINT       NOT NULL,
    ward               VARCHAR(100) NOT NULL,
    severity           VARCHAR(20),
    channel            VARCHAR(20)  NOT NULL,
    recipient_username VARCHAR(100) NOT NULL,
    recipient_address  VARCHAR(150),
    message            VARCHAR(2000),
    reason             VARCHAR(300),
    status             VARCHAR(20)  NOT NULL,
    error_message      VARCHAR(500),
    sent_at            DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_alert_channel CHECK (channel IN ('EMAIL', 'WHATSAPP')),
    CONSTRAINT chk_alert_status CHECK (status IN ('SENT', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_alert_event ON alert_log (event_id);
CREATE INDEX idx_alert_hazard ON alert_log (hazard);
CREATE INDEX idx_alert_sent_at ON alert_log (sent_at);
