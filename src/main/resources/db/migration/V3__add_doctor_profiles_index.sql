-- ========================================================
-- V3: BỔ SUNG INDEX TỐI ƯU TÌM KIẾM HỒ SƠ BÁC SĨ
-- Migration: MySQL 8.0 InnoDB
-- ========================================================

CREATE INDEX idx_doctor_profiles_specialty_available
    ON doctor_profiles (specialty_id, is_available);
