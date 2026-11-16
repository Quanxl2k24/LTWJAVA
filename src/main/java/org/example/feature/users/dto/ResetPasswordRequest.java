package org.example.feature.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu đặt lại mật khẩu người dùng bởi Quản trị viên")
public class ResetPasswordRequest {

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 50, message = "Mật khẩu mới phải có từ 6 đến 50 ký tự")
    @Schema(description = "Mật khẩu mới được chỉ định cấp cho người dùng", example = "ResetPass@2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}

