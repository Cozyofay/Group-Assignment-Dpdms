-- Seed data for demonstrations. Usernames match the auth-service demo users.
INSERT INTO flood_incident
(ward, district, province, occurred_at, reporter_username, severity, status, latitude, longitude,
 review_comment, reviewed_by, reviewed_at, created_at, updated_at, version,
 peak_water_level_metres, river_basin, households_displaced, area_flooded_hectares, inundation_duration_days)
VALUES
('Ward 1', 'Rushinga', 'Mashonaland Central', '2026-01-14 06:30:00', 'recorder.flood.ward1', 'HIGH', 'APPROVED',
 -16.6231, 32.0854, 'Verified with the district office', 'supervisor.flood', '2026-01-15 09:10:00',
 '2026-01-14 07:05:00', '2026-01-15 09:10:00', 0, 2.8, 'Mazowe', 140, 320.5, 6),

('Ward 2', 'Rushinga', 'Mashonaland Central', '2026-02-02 18:45:00', 'recorder.flood.ward2', 'MODERATE', 'APPROVED',
 -16.5402, 32.1523, NULL, 'supervisor.flood', '2026-02-03 08:00:00',
 '2026-02-02 19:20:00', '2026-02-03 08:00:00', 0, 1.4, 'Ruya', 35, 88.0, 3),

('Ward 1', 'Rushinga', 'Mashonaland Central', '2026-03-11 22:15:00', 'recorder.flood.ward1', 'CRITICAL', 'PENDING',
 -16.6105, 32.0712, NULL, NULL, NULL,
 '2026-03-11 23:00:00', '2026-03-11 23:00:00', 0, 3.6, 'Mazowe', 210, 610.0, 9);
