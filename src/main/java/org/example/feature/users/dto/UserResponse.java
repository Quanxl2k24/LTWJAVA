package org.example.feature.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.feature.role.dto.RoleResponse;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết người dùng")
public class UserResponse {

    @Schema(description = "ID định danh người dùng", example = "1")
    private Long id;

    @Schema(description = "Số điện thoại đăng nhập và liên hệ", example = "0987654321")
    private String phone;

    @Schema(description = "Địa chỉ email người dùng", example = "bacsi.nguyen@hospital.com")
    private String email;

    @Schema(description = "Họ và tên đầy đủ", example = "Bác sĩ Nguyễn Văn A")
    private String fullName;

    @Schema(description = "Giới tính (male, female, other)", example = "male")
    private String gender;

    @Schema(description = "Ngày tháng năm sinh", example = "1985-05-20")
    private LocalDate dateOfBirth;

    @Schema(description = "Địa chỉ cư trú", example = "Số 123 Đường Giải Phóng, Hà Nội")
    private String address;

    @Schema(description = "Trạng thái hoạt động của tài khoản (true: hoạt động, false: bị khóa)", example = "true")
    private Boolean isActive;

    @Schema(description = "Thông tin vai trò của người dùng trong hệ thống")
    private RoleResponse role;

    @Schema(description = "Thời điểm tạo tài khoản", example = "2026-11-01T08:30:00Z")
    private Instant createdAt;

    @Schema(description = "Thời điểm cập nhật tài khoản gần nhất", example = "2026-11-10T14:20:00Z")
    private Instant updatedAt;
}

