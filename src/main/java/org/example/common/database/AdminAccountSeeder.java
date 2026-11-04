package org.example.common.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountSeeder {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.email:admin@hospital.com}")
    private String adminEmail;

    @Value("${app.seed.admin.password:Admin@123456}")
    private String adminPassword;

    @Value("${app.seed.admin.phone:0987654321}")
    private String adminPhone;

    @Value("${app.seed.admin.full-name:System Administrator}")
    private String adminFullName;

    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void seedDefaultAdmin() {
        try {
            // 1. Kiểm tra bảng users có tồn tại không (phòng trường hợp Flyway chưa chạy trong test)
            Integer tableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'users'",
                    Integer.class
            );

            if (tableExists == null || tableExists == 0) {
                log.warn("⚠️ [SEED] Bảng 'users' chưa sẵn sàng trong database, bỏ qua seed tài khoản admin.");
                return;
            }

            // 2. Kiểm tra xem tài khoản admin đã tồn tại chưa (dựa theo email hoặc phone)
            Integer userCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE email = ? OR phone = ?",
                    Integer.class,
                    adminEmail,
                    adminPhone
            );

            if (userCount != null && userCount > 0) {
                log.info("ℹ️ [SEED] Tài khoản Admin (Email: {}) đã tồn tại trong database. Bỏ qua seed.", adminEmail);
                return;
            }

            // 3. Lấy role_id của vai trò ADMIN
            Integer adminRoleId = jdbcTemplate.queryForObject(
                    "SELECT id FROM roles WHERE code = 'ADMIN' LIMIT 1",
                    Integer.class
            );

            if (adminRoleId == null) {
                adminRoleId = 1;
            }

            // 4. Mã hóa mật khẩu an toàn với BCrypt
            String hashedPassword = passwordEncoder.encode(adminPassword);

            // 5. Thêm bản ghi admin vào bảng users
            String insertSql = "INSERT INTO users (role_id, phone, email, password_hash, full_name, gender, is_active) " +
                    "VALUES (?, ?, ?, ?, ?, 'other', true)";

            jdbcTemplate.update(insertSql, adminRoleId, adminPhone, adminEmail, hashedPassword, adminFullName);

            log.info("=================================================================");
            log.info("👑 [SEED] TẠO THÀNH CÔNG TÀI KHOẢN ADMIN MẶC ĐỊNH TỪ FILE .ENV:");
            log.info("   -> Full Name : {}", adminFullName);
            log.info("   -> Email     : {}", adminEmail);
            log.info("   -> Phone     : {}", adminPhone);
            log.info("   -> Role      : ADMIN (ID: {})", adminRoleId);
            log.info("   -> Password  : [ĐÃ MÃ HÓA BCRYPT AN TOÀN TRONG DATABASE]");
            log.info("=================================================================");

        } catch (Exception e) {
            log.error("❌ [SEED] Không thể seed tài khoản Admin: {}", e.getMessage(), e);
        }
    }
}

