-- Seed data for demonstrations. Usernames match the auth-service demo users.
INSERT INTO zoonotic_disease_incident
(ward, district, province, occurred_at, reporter_username, severity, status, latitude, longitude,
 review_comment, reviewed_by, reviewed_at, created_at, updated_at, version, pathogen_name, animal_species_affected, confirmed_human_cases, confirmed_animal_cases, classification)
VALUES
('Ward 1', 'Rushinga', 'Mashonaland Central', '2026-01-20 09:00:00', 'recorder.zoonotic.ward1', 'HIGH', 'APPROVED',
 -16.6231, 32.0854, 'Verified with the district office', 'supervisor.zoonotic', '2026-01-21 10:00:00',
 '2026-01-20 09:40:00', '2026-01-21 10:00:00', 0, 'Anthrax', 'Cattle', 6, 35, 'OUTBREAK'),

('Ward 2', 'Rushinga', 'Mashonaland Central', '2026-02-09 14:20:00', 'recorder.zoonotic.ward1', 'MODERATE', 'APPROVED',
 -16.5402, 32.1523, NULL, 'supervisor.zoonotic', '2026-02-10 08:30:00',
 '2026-02-09 15:00:00', '2026-02-10 08:30:00', 0, 'Rabies', 'Dogs', 1, 4, 'CLUSTER'),

('Ward 1', 'Rushinga', 'Mashonaland Central', '2026-03-05 11:05:00', 'recorder.zoonotic.ward1', 'CRITICAL', 'PENDING',
 -16.6105, 32.0712, NULL, NULL, NULL,
 '2026-03-05 11:45:00', '2026-03-05 11:45:00', 0, 'Brucellosis', 'Goats', 14, 77, 'OUTBREAK');
