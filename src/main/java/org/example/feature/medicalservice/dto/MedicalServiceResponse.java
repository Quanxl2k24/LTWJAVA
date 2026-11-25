package org.example.feature.medicalservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết dịch vụ y tế")
public class MedicalServiceResponse {

    @Schema(description = "ID định danh dịch vụ", example = "1")
    private Integer id;

    @Schema(description = "Tên dịch vụ khám", example = "Siêu âm tim Doppler màu")
    private String name;

    @Schema(description = "Giá dịch vụ (VNĐ)", example = "250000.00")
    private BigDecimal price;

    @Schema(description = "Thời gian khám dự kiến (phút)", example = "20")
    private Integer estimatedDurationMin;

    @Schema(description = "Mô tả chi tiết và lưu ý chuẩn bị", example = "Không cần nhịn ăn trước khi siêu âm.")
    private String description;

    @Schema(description = "Trạng thái hoạt động", example = "true")
    private Boolean isActive;

    @Schema(description = "Thông tin chuyên khoa phụ trách (null nếu là dịch vụ dùng chung / tổng quát)")
    private SpecialtySummary specialty;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Thông tin tóm tắt chuyên khoa")
    public static class SpecialtySummary {
        @Schema(description = "ID chuyên khoa", example = "1")
        private Integer id;

        @Schema(description = "Tên chuyên khoa", example = "Khoa Tim Mạch")
        private String name;

        @Schema(description = "Slug chuyên khoa", example = "khoa-tim-mach")
        private String slug;
    }
}
