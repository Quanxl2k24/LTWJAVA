package org.example.feature.specialty.dto;

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
@Schema(description = "Dữ liệu yêu cầu bật / tắt trạng thái hoạt động của chuyên khoa")
public class UpdateSpecialtyStatusRequest {

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    @Schema(description = "Trạng thái hoạt động của chuyên khoa (true: Bật / Hoạt động, false: Tắt / Ẩn)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isActive;
}

