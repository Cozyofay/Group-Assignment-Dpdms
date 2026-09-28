-- Seed data for demonstrations. Usernames match the auth-service demo users.
INSERT INTO mining_accident_incident
(ward, district, province, occurred_at, reporter_username, severity, status, latitude, longitude,
 review_comment, reviewed_by, reviewed_at, created_at, updated_at, version, mine_name, mine_type, accident_type, trapped_or_injured_miners, fatalities, rescue_ongoing)
VALUES
('Ward 1', 'Rushinga', 'Mashonaland Central', '2026-01-20 09:00:00', 'recorder.mining.ward1', 'HIGH', 'APPROVED',
 -16.6231, 32.0854, 'Verified with the district office', 'supervisor.mining', '2026-01-21 10:00:00',
 '2026-01-20 09:40:00', '2026-01-21 10:00:00', 0, 'Shamva Gold Mine', 'FORMAL', 'COLLAPSE', 4, 1, FALSE),

('Ward 2', 'Rushinga', 'Mashonaland Central', '2026-02-09 14:20:00', 'recorder.mining.ward1', 'MODERATE', 'APPROVED',
 -16.5402, 32.1523, NULL, 'supervisor.mining', '2026-02-10 08:30:00',
 '2026-02-09 15:00:00', '2026-02-10 08:30:00', 0, 'Kanyemba Claim', 'ARTISANAL', 'FLOODING', 9, 0, TRUE),

('Ward 1', 'Rushinga', 'Mashonaland Central', '2026-03-05 11:05:00', 'recorder.mining.ward1', 'CRITICAL', 'PENDING',
 -16.6105, 32.0712, NULL, NULL, NULL,
 '2026-03-05 11:45:00', '2026-03-05 11:45:00', 0, 'Chimanda Shaft', 'ARTISANAL', 'GAS_EXPLOSION', 2, 3, FALSE);
