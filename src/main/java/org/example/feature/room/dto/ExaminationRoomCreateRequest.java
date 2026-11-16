package org.example.feature.room.dto;

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
@Schema(description = "Dữ liệu yêu cầu tạo mới phòng khám")
public class ExaminationRoomCreateRequest {

    @NotBlank(message = "Số phòng khám không được để trống")
    @Size(min = 1, max = 20, message = "Số phòng khám phải có độ dài từ 1 đến 20 ký tự")
    @Schema(description = "Số phòng hoặc mã phòng khám", example = "P.201", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roomNumber;

    @Size(max = 50, message = "Tên khu nhà / tòa nhà không được vượt quá 50 ký tự")
    @Schema(description = "Tòa nhà hoặc khu vực đặt phòng khám", example = "Khu A - Tầng 2")
    private String building;

    @Schema(description = "Trạng thái sẵn sàng hoạt động ngay (mặc định: true)", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isActive = true;
}

