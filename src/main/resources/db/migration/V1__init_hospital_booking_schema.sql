-- ========================================================
-- DATABASE SCHEMA: HỆ THỐNG ĐẶT LỊCH KHÁM BỆNH VIỆN
-- Flyway Migration V1: Khởi tạo toàn bộ cấu trúc bảng và dữ liệu vai trò ban đầu
-- ========================================================

-- ========================================================
-- NHÓM 1: TÀI KHOẢN, PHÂN QUYỀN & HỒ SƠ
-- ========================================================

-- 1. Bảng vai trò
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dữ liệu khởi tạo vai trò mặc định
INSERT INTO roles (id, code, name, description) VALUES
(1, 'ADMIN', 'Quản trị viên', 'Toàn quyền quản trị hệ thống'),
(2, 'DOCTOR', 'Bác sĩ', 'Xem lịch khám, ca trực, hồ sơ bệnh nhân'),
(3, 'STAFF', 'Nhân viên / Lễ tân', 'Tiếp đón, xếp lịch hẹn, thu tiền'),
(4, 'PATIENT', 'Bệnh nhân', 'Đặt lịch, xem lịch sử khám cá nhân')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- 2. Bảng người dùng hệ thống
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id INT NOT NULL DEFAULT 4,
    phone VARCHAR(15) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    date_of_birth DATE,
    address TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_gender CHECK (gender IN ('male', 'female', 'other')),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_users_role ON users(role_id);

-- 3. Hồ sơ bệnh nhân (1 User có thể quản lý nhiều hồ sơ: bản thân, người nhà)
CREATE TABLE IF NOT EXISTS patient_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    identity_card VARCHAR(20),
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    address TEXT,
    medical_history TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_patient_gender CHECK (gender IN ('male', 'female', 'other')),
    CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_patient_user ON patient_profiles(user_id);

-- ========================================================
-- NHÓM 2: CHUYÊN KHOA, PHÒNG KHÁM & DỊCH VỤ
-- ========================================================

-- 4. Bảng chuyên khoa
CREATE TABLE IF NOT EXISTS specialties (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) UNIQUE NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Phòng khám thực tế tại bệnh viện
CREATE TABLE IF NOT EXISTS examination_rooms (
    id INT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL,
    building VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Thông tin chi tiết bác sĩ (Quan hệ 1-1 với users)
CREATE TABLE IF NOT EXISTS doctor_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    specialty_id INT NOT NULL,
    title VARCHAR(50),
    experience_years INT DEFAULT 0,
    biography TEXT,
    consultation_fee DECIMAL(12, 2) NOT NULL DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_doctor_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_doctor_specialty ON doctor_profiles(specialty_id);

-- 7. Dịch vụ khám / gói khám
CREATE TABLE IF NOT EXISTS medical_services (
    id INT AUTO_INCREMENT PRIMARY KEY,
    specialty_id INT,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    estimated_duration_min INT DEFAULT 15,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_service_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- NHÓM 3: CA KHÁM & LỊCH LÀM VIỆC CỦA BÁC SĨ
-- ========================================================

-- 8. Khung giờ cố định trong ngày
CREATE TABLE IF NOT EXISTS time_slots (
    id INT AUTO_INCREMENT PRIMARY KEY,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    session VARCHAR(15),
    CONSTRAINT chk_time_session CHECK (session IN ('morning', 'afternoon', 'evening')),
    CONSTRAINT chk_time_order CHECK (start_time < end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Lịch phân ca chi tiết của bác sĩ theo ngày
CREATE TABLE IF NOT EXISTS doctor_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    room_id INT,
    work_date DATE NOT NULL,
    slot_id INT NOT NULL,
    max_patients INT DEFAULT 1,
    status VARCHAR(20) DEFAULT 'available',
    CONSTRAINT chk_schedule_status CHECK (status IN ('available', 'booked', 'off')),
    CONSTRAINT uq_doctor_schedule UNIQUE (doctor_id, work_date, slot_id),
    CONSTRAINT fk_schedule_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_room FOREIGN KEY (room_id) REFERENCES examination_rooms(id) ON DELETE SET NULL,
    CONSTRAINT fk_schedule_slot FOREIGN KEY (slot_id) REFERENCES time_slots(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_doctor_schedules_lookup ON doctor_schedules (doctor_id, work_date, status);

-- ========================================================
-- NHÓM 4: LỊCH HẸN & THANH TOÁN
-- ========================================================

-- 10. Lịch hẹn khám
CREATE TABLE IF NOT EXISTS appointments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_code VARCHAR(20) UNIQUE NOT NULL,
    patient_profile_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    service_id INT,
    appointment_date DATE NOT NULL,
    queue_number INT,
    symptoms TEXT,
    status VARCHAR(20) DEFAULT 'pending',
    cancellation_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_appointment_status CHECK (status IN ('pending', 'confirmed', 'completed', 'cancelled', 'no_show')),
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_profile_id) REFERENCES patient_profiles(id),
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor_profiles(id),
    CONSTRAINT fk_appointment_schedule FOREIGN KEY (schedule_id) REFERENCES doctor_schedules(id),
    CONSTRAINT fk_appointment_service FOREIGN KEY (service_id) REFERENCES medical_services(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_appointments_patient ON appointments (patient_profile_id, appointment_date);
CREATE INDEX idx_appointments_code ON appointments (appointment_code);

-- 11. Nhật ký thay đổi trạng thái cuộc hẹn
CREATE TABLE IF NOT EXISTS appointment_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by BIGINT,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_applog_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
    CONSTRAINT fk_applog_changed_by FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Giao dịch thanh toán (Quan hệ 1-1 với appointments)
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT UNIQUE NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    payment_method VARCHAR(30) NOT NULL,
    payment_status VARCHAR(20) DEFAULT 'unpaid',
    transaction_code VARCHAR(100),
    paid_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('cash', 'bank_transfer', 'momo', 'vnpay', 'credit_card')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('unpaid', 'paid', 'refunded', 'failed')),
    CONSTRAINT fk_payment_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================================
-- NHÓM 5: BẢO MẬT & QUẢN LÝ PHIÊN (AUTH & REFRESH TOKENS)
-- ========================================================

-- 13. Quản lý Refresh Token & Phiên đăng nhập
CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    device_id VARCHAR(100),
    device_name VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent TEXT,
    is_revoked BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sessions_token_hash ON user_sessions(token_hash);
CREATE INDEX idx_sessions_user_revoked ON user_sessions(user_id, is_revoked);
CREATE INDEX idx_sessions_expiry ON user_sessions(expires_at);

-- 14. Nhật ký kiểm tra bảo mật (Audit logs)
CREATE TABLE IF NOT EXISTS auth_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(30) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    attempted_phone_or_email VARCHAR(100),
    failure_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_user_time ON auth_audit_logs(user_id, created_at);
CREATE INDEX idx_audit_ip_time ON auth_audit_logs(ip_address, created_at);

