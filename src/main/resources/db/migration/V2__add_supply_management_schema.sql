-- ========================================================
-- V2: QUẢN LÝ VẬT TƯ Y TẾ & TỒN KHO
-- Migration: MySQL 8.0 InnoDB
-- ========================================================

-- ========================================================
-- CẬP NHẬT MÔ TẢ ROLE (đã tồn tại từ V1)
-- ========================================================
INSERT INTO roles (id, code, name, description) VALUES
    (1, 'ADMIN',   'Quản trị viên',               'Toàn quyền quản trị hệ thống và kho vật tư'),
    (2, 'DOCTOR',  'Bác sĩ',                       'Khám bệnh, xem lịch hẹn và kê vật tư tiêu hao'),
    (3, 'STAFF',   'Nhân viên / Lễ tân / Thủ kho', 'Điều phối lịch hẹn, thu ngân và xuất nhập kho'),
    (4, 'PATIENT', 'Bệnh nhân',                    'Đặt lịch khám và tra cứu hồ sơ')
ON DUPLICATE KEY UPDATE
    name        = VALUES(name),
    description = VALUES(description);

-- ========================================================
-- 15. DANH MỤC VẬT TƯ Y TẾ (hỗ trợ đa cấp cha - con)
-- ========================================================
CREATE TABLE supply_categories (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    parent_id   INT          NULL,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL,
    description TEXT         NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_supply_categories_slug UNIQUE (slug),
    CONSTRAINT fk_supply_categories_parent
        FOREIGN KEY (parent_id) REFERENCES supply_categories (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 16. NHÀ CUNG CẤP VẬT TƯ
-- ========================================================
CREATE TABLE suppliers (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(30)  NOT NULL,
    name           VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100) NULL,
    phone          VARCHAR(15)  NULL,
    email          VARCHAR(100) NULL,
    tax_code       VARCHAR(20)  NULL,
    address        TEXT         NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_suppliers_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 17. DANH MỤC VẬT TƯ Y TẾ
-- ========================================================
CREATE TABLE medical_supplies (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id     INT             NULL,
    sku             VARCHAR(50)     NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    unit            VARCHAR(30)     NOT NULL,
    min_stock_level INT             NOT NULL DEFAULT 0  CHECK (min_stock_level >= 0),
    total_quantity  INT             NOT NULL DEFAULT 0  CHECK (total_quantity >= 0),
    unit_price      DECIMAL(12, 2)  NOT NULL DEFAULT 0  CHECK (unit_price >= 0),
    is_chargeable   BOOLEAN         NOT NULL DEFAULT FALSE,
    description     TEXT            NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_medical_supplies_sku UNIQUE (sku),
    CONSTRAINT fk_medical_supplies_category
        FOREIGN KEY (category_id) REFERENCES supply_categories (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- 18. LÔ NHẬP VẬT TƯ
-- ========================================================
CREATE TABLE supply_batches (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    supply_id        BIGINT         NOT NULL,
    supplier_id      INT            NULL,
    batch_number     VARCHAR(50)    NOT NULL,
    initial_quantity INT            NOT NULL CHECK (initial_quantity > 0),
    remaining_quantity INT          NOT NULL CHECK (remaining_quantity >= 0),
    import_price     DECIMAL(12, 2) NOT NULL DEFAULT 0 CHECK (import_price >= 0),
    manufacturing_date DATE         NULL,
    expiration_date  DATE           NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'active'
                         CHECK (status IN ('active', 'depleted', 'expired', 'recalled')),
    received_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_supply_batches_supply_batch UNIQUE (supply_id, batch_number),
    CONSTRAINT fk_supply_batches_supply
        FOREIGN KEY (supply_id) REFERENCES medical_supplies (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_supply_batches_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_supply_batches_supply   ON supply_batches (supply_id);
CREATE INDEX idx_supply_batches_supplier ON supply_batches (supplier_id);
CREATE INDEX idx_supply_batches_status   ON supply_batches (status, expiration_date);

-- ========================================================
-- 19. GIAO DỊCH XUẤT / NHẬP KHO
-- ========================================================
CREATE TABLE inventory_transactions (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_code VARCHAR(30)  NOT NULL,
    supply_id        BIGINT       NOT NULL,
    batch_id         BIGINT       NULL,
    transaction_type VARCHAR(20)  NOT NULL
                         CHECK (transaction_type IN ('import', 'export', 'adjustment', 'return', 'disposal')),
    quantity         INT          NOT NULL,
    room_id          INT          NULL,
    appointment_id   BIGINT       NULL,
    performed_by     BIGINT       NULL,
    note             TEXT         NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_inventory_transactions_code UNIQUE (transaction_code),
    CONSTRAINT fk_inv_tx_supply
        FOREIGN KEY (supply_id) REFERENCES medical_supplies (id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_inv_tx_batch
        FOREIGN KEY (batch_id) REFERENCES supply_batches (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_inv_tx_room
        FOREIGN KEY (room_id) REFERENCES examination_rooms (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_inv_tx_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_inv_tx_performed_by
        FOREIGN KEY (performed_by) REFERENCES users (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_inv_tx_supply      ON inventory_transactions (supply_id, created_at);
CREATE INDEX idx_inv_tx_code        ON inventory_transactions (transaction_code);
CREATE INDEX idx_inv_tx_appointment ON inventory_transactions (appointment_id);

-- ========================================================
-- 20. ĐỊNH MỨC VẬT TƯ THEO DỊCH VỤ
-- ========================================================
CREATE TABLE service_supply_norms (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    service_id        INT    NOT NULL,
    supply_id         BIGINT NOT NULL,
    standard_quantity INT    NOT NULL DEFAULT 1 CHECK (standard_quantity > 0),
    CONSTRAINT uq_service_supply_norms UNIQUE (service_id, supply_id),
    CONSTRAINT fk_ssn_service
        FOREIGN KEY (service_id) REFERENCES medical_services (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_ssn_supply
        FOREIGN KEY (supply_id) REFERENCES medical_supplies (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_service_supply_norms_service ON service_supply_norms (service_id);
CREATE INDEX idx_service_supply_norms_supply  ON service_supply_norms (supply_id);

