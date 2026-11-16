package org.example.feature.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết phòng khám")
public class ExaminationRoomResponse {

    @Schema(description = "ID định danh phòng khám", example = "1")
    private Integer id;

    @Schema(description = "Số phòng khám hoặc tên phòng", example = "P.201")
    private String roomNumber;

    @Schema(description = "Tòa nhà / Khu vực đặt phòng khám", example = "Khu A - Tầng 2")
    private String building;

    @Schema(description = "Trạng thái hoạt động (true: Đang hoạt động / Sẵn sàng đón tiếp, false: Đang bảo trì / Tạm ngưng)", example = "true")
    private Boolean isActive;
}

