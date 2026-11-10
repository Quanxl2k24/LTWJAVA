package org.example.common.database;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.feature.auth.model.Role;
import org.example.feature.auth.repository.RoleRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleSeeder {

    private final RoleRepository roleRepository;

    private record RoleDefinition(String code, String name, String description) {}

    private static final List<RoleDefinition> DEFAULT_ROLES = List.of(
            new RoleDefinition("ADMIN", "Admin", "Toàn quyền quản trị hệ thống"),
            new RoleDefinition("DOCTOR", "Bác sĩ", "Bác sĩ khám chữa bệnh, xem ca trực và hồ sơ khám"),
            new RoleDefinition("STAFF", "Lễ tân", "Nhân viên lễ tân tiếp đón, điều phối lịch hẹn và thu viện phí"),
            new RoleDefinition("PATIENT", "Khách hàng", "Khách hàng đăng ký tài khoản, tra cứu dịch vụ và đặt lịch khám")
    );

    @EventListener(ApplicationReadyEvent.class)
    @Order(1) // Chạy trước AdminAccountSeeder (@Order(10))
    @Transactional
    public void seedRoles() {
        try {
            log.info("=================================================================");
            log.info("🔄 [SEED] ĐANG ĐỒNG BỘ CÁC VAI TRÒ (ROLES) MẶC ĐỊNH HỆ THỐNG...");

            for (RoleDefinition def : DEFAULT_ROLES) {
                roleRepository.findByCode(def.code()).ifPresentOrElse(
                        existingRole -> {
                            // Cập nhật tên và mô tả nếu cần
                            existingRole.setName(def.name());
                            existingRole.setDescription(def.description());
                            roleRepository.save(existingRole);
                        },
                        () -> {
                            // Tạo mới nếu chưa có
                            Role newRole = Role.builder()
                                    .code(def.code())
                                    .name(def.name())
                                    .description(def.description())
                                    .build();
                            roleRepository.save(newRole);
                        }
                );
            }

            log.info("🎭 [SEED] KHỞI TẠO THÀNH CÔNG 4 VAI TRÒ:");
            for (RoleDefinition def : DEFAULT_ROLES) {
                log.info("   -> [{}] : {}", def.code(), def.name());
            }
            log.info("=================================================================");

        } catch (Exception e) {
            log.warn("⚠️ [SEED] Không thể seed roles (có thể bảng chưa sẵn sàng): {}", e.getMessage());
        }
    }
}

