package org.example.feature.medicalservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu bật/tắt trạng thái dịch vụ y tế")
public class UpdateMedicalServiceStatusRequest {

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    @Schema(description = "Trạng thái mới (true: Mở dịch vụ, false: Ngưng cung cấp)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isActive;
}
