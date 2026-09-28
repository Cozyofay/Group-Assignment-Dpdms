-- Seed data for demonstrations. Usernames match the auth-service demo users.
INSERT INTO fire_incident
(ward, district, province, occurred_at, reporter_username, severity, status, latitude, longitude,
 review_comment, reviewed_by, reviewed_at, created_at, updated_at, version, area_burned_hectares, suspected_cause, injuries_or_fatalities, structures_destroyed, still_active)
VALUES
('Ward 1', 'Rushinga', 'Mashonaland Central', '2026-01-20 09:00:00', 'recorder.fire.ward1', 'HIGH', 'APPROVED',
 -16.6231, 32.0854, 'Verified with the district office', 'supervisor.fire', '2026-01-21 10:00:00',
 '2026-01-20 09:40:00', '2026-01-21 10:00:00', 0, 156.0, 'NATURAL', 3, 9, FALSE),

('Ward 2', 'Rushinga', 'Mashonaland Central', '2026-02-09 14:20:00', 'recorder.fire.ward1', 'MODERATE', 'APPROVED',
 -16.5402, 32.1523, NULL, 'supervisor.fire', '2026-02-10 08:30:00',
 '2026-02-09 15:00:00', '2026-02-10 08:30:00', 0, 12.5, 'ACCIDENTAL', 0, 1, FALSE),

('Ward 1', 'Rushinga', 'Mashonaland Central', '2026-03-05 11:05:00', 'recorder.fire.ward1', 'CRITICAL', 'PENDING',
 -16.6105, 32.0712, NULL, NULL, NULL,
 '2026-03-05 11:45:00', '2026-03-05 11:45:00', 0, 430.0, 'DELIBERATE', 7, 23, TRUE);
