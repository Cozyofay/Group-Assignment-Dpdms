-- auth-service schema (database: dpdms_auth). Applied automatically by Flyway on startup.
CREATE TABLE users (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    username              VARCHAR(100) NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    full_name             VARCHAR(150) NOT NULL,
    email                 VARCHAR(150),
    phone_number          VARCHAR(20),
    role                  VARCHAR(30)  NOT NULL,
    hazard                VARCHAR(30),
    ward                  VARCHAR(100),
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    receive_alerts        BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          DATETIME,
    last_login_at         DATETIME,
    created_at            DATETIME     NOT NULL,
    updated_at            DATETIME,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_role CHECK (role IN ('WARD_RECORDER', 'PROVINCIAL_SUPERVISOR', 'PROVINCIAL_ADMIN', 'NATIONAL_VIEWER')),
    CONSTRAINT chk_users_hazard CHECK (hazard IS NULL OR hazard IN ('FLOOD', 'DROUGHT', 'FIRE', 'ZOONOTIC_DISEASE', 'MINING_ACCIDENT'))
);

CREATE INDEX idx_users_role_hazard ON users (role, hazard);
