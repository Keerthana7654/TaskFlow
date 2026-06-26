-- V3__fix_admin_password.sql
-- The hash originally inserted in V2 did not actually correspond to 'Admin@123'
-- (it was a typo'd/fabricated hash, not a real BCrypt output). This migration
-- corrects it with a verified hash so admin@taskflow.com / Admin@123 works.
--
-- Verified with: bcrypt.checkpw(b"Admin@123", new_hash) -> True

UPDATE users
SET password_hash = '$2b$12$.6Hws5KbX3Am4n3y0UkDcOtgXRxmIuKrDfanhsEmDtvNmnTrAOiKW'
WHERE email = 'admin@taskflow.com';
