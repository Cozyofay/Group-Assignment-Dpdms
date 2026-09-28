-- Seed data for demonstrations. Usernames match the auth-service demo users.
INSERT INTO drought_incident
(ward, district, province, occurred_at, reporter_username, severity, status, latitude, longitude,
 review_comment, reviewed_by, reviewed_at, created_at, updated_at, version, rainfall_deficit_mm, consecutive_dry_days, crop_failure_percentage, people_facing_water_shortages, livestock_mortality_count)
VALUES
('Ward 1', 'Rushinga', 'Mashonaland Central', '2026-01-20 09:00:00', 'recorder.drought.ward1', 'HIGH', 'APPROVED',
 -16.6231, 32.0854, 'Verified with the district office', 'supervisor.drought', '2026-01-21 10:00:00',
 '2026-01-20 09:40:00', '2026-01-21 10:00:00', 0, 420.0, 78, 71.5, 3200, 260),

('Ward 2', 'Rushinga', 'Mashonaland Central', '2026-02-09 14:20:00', 'recorder.drought.ward1', 'MODERATE', 'APPROVED',
 -16.5402, 32.1523, NULL, 'supervisor.drought', '2026-02-10 08:30:00',
 '2026-02-09 15:00:00', '2026-02-10 08:30:00', 0, 180.5, 41, 38.0, 950, 45),

('Ward 1', 'Rushinga', 'Mashonaland Central', '2026-03-05 11:05:00', 'recorder.drought.ward1', 'CRITICAL', 'PENDING',
 -16.6105, 32.0712, NULL, NULL, NULL,
 '2026-03-05 11:45:00', '2026-03-05 11:45:00', 0, 515.0, 96, 88.0, 5100, 410);
