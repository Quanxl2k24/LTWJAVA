package org.example.common.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.feature.auth.model.Role;
import org.example.feature.auth.model.User;
import org.example.feature.auth.repository.RoleRepository;
import org.example.feature.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountSeeder {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
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
    @Transactional
    public void seedDefaultAdmin() {
        try {
            // 1. Kiểm tra xem tài khoản admin đã tồn tại chưa (dựa theo email hoặc phone)
            if (userRepository.existsByEmail(adminEmail) || userRepository.existsByPhone(adminPhone)) {
                log.info("ℹ️ [SEED] Tài khoản Admin (Email: {}) đã tồn tại trong database. Bỏ qua seed.", adminEmail);
                return;
            }

            // 2. Tìm Role ADMIN
            Role adminRole = roleRepository.findByCode("ADMIN")
                    .orElseGet(() -> roleRepository.findAll().stream().findFirst().orElse(null));

            if (adminRole == null) {
                log.warn("⚠️ [SEED] Không tìm thấy vai trò ADMIN để liên kết cho tài khoản quản trị.");
                return;
            }

            // 3. Tạo tài khoản Admin mặc định
            User adminUser = User.builder()
                    .role(adminRole)
                    .phone(adminPhone)
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .fullName(adminFullName)
                    .gender("other")
                    .isActive(true)
                    .build();

            userRepository.save(adminUser);

            log.info("=================================================================");
            log.info("👑 [SEED] TẠO THÀNH CÔNG TÀI KHOẢN ADMIN MẶC ĐỊNH TỪ FILE .ENV:");
            log.info("   -> Full Name : {}", adminFullName);
            log.info("   -> Email     : {}", adminEmail);
            log.info("   -> Phone     : {}", adminPhone);
            log.info("   -> Role      : {} (ID: {})", adminRole.getCode(), adminRole.getId());
            log.info("   -> Password  : [ĐÃ MÃ HÓA BCRYPT AN TOÀN TRONG DATABASE]");
            log.info("=================================================================");

        } catch (Exception e) {
            log.error("❌ [SEED] Không thể seed tài khoản Admin: {}", e.getMessage(), e);
        }
    }
}

