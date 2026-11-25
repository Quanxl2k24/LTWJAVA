package org.example.feature.doctorprofile.dto;

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
@Schema(description = "Dữ liệu yêu cầu bật/tắt trạng thái nhận khám của bác sĩ")
public class UpdateDoctorAvailabilityRequest {

    @NotNull(message = "Trạng thái sẵn sàng không được để trống")
    @Schema(description = "Trạng thái nhận khám (true: Đang nhận lịch, false: Tạm ngưng nhận lịch)", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isAvailable;
}
