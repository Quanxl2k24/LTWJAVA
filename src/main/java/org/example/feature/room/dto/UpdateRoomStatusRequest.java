package org.example.feature.room.dto;

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
@Schema(description = "Dữ liệu yêu cầu thay đổi trạng thái hoạt động / bảo trì của phòng khám")
public class UpdateRoomStatusRequest {

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    @Schema(description = "Trạng thái phòng khám (true: Hoạt động / Sẵn sàng đón bệnh nhân, false: Tạm ngưng / Đang bảo trì)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isActive;
}

