-- ========================================================
-- V4: BỔ SUNG INDEX TỐI ƯU TRA CỨU DỊCH VỤ Y TẾ
-- Migration: MySQL 8.0 InnoDB
-- ========================================================

CREATE INDEX idx_medical_services_specialty_active
    ON medical_services (specialty_id, is_active);

CREATE INDEX idx_medical_services_active_price
    ON medical_services (is_active, price);
