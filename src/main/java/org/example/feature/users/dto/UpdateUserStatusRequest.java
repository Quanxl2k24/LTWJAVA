package org.example.feature.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu thay đổi trạng thái kích hoạt / khóa tài khoản")
public class UpdateUserStatusRequest {

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    @Schema(description = "Trạng thái mới của tài khoản (true: Kích hoạt / Mở khóa, false: Tạm khóa)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isActive;

    @Size(max = 255, message = "Lý do thay đổi trạng thái không được vượt quá 255 ký tự")
    @Schema(description = "Lý do khóa hoặc mở khóa tài khoản", example = "Khóa tạm thời theo yêu cầu kiểm tra nội bộ")
    private String reason;
}

