-- DPDMS: one database (schema) per micro-service, as required by the brief.
-- Run once as the MySQL root user (e.g. in MySQL Workbench).
-- IMPORTANT: change 'replace-me' below to the same value as DB_PASSWORD in your .env file.

CREATE DATABASE IF NOT EXISTS dpdms_auth     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS dpdms_flood    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS dpdms_drought  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS dpdms_fire     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS dpdms_zoonotic CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS dpdms_mining   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS dpdms_alert    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'dpdms'@'localhost' IDENTIFIED BY 'replace-me';
CREATE USER IF NOT EXISTS 'dpdms'@'%'         IDENTIFIED BY 'replace-me';

GRANT ALL PRIVILEGES ON dpdms_auth.*     TO 'dpdms'@'localhost', 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_flood.*    TO 'dpdms'@'localhost', 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_drought.*  TO 'dpdms'@'localhost', 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_fire.*     TO 'dpdms'@'localhost', 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_zoonotic.* TO 'dpdms'@'localhost', 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_mining.*   TO 'dpdms'@'localhost', 'dpdms'@'%';
GRANT ALL PRIVILEGES ON dpdms_alert.*    TO 'dpdms'@'localhost', 'dpdms'@'%';

FLUSH PRIVILEGES;
