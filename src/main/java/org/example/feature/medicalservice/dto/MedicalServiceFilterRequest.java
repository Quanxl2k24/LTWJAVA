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
@Schema(description = "Bộ lọc tìm kiếm danh mục dịch vụ y tế")
public class MedicalServiceFilterRequest {

    @Schema(description = "Từ khóa tìm kiếm theo tên dịch vụ", example = "Siêu âm")
    private String keyword;

    @Schema(description = "Lọc theo ID chuyên khoa", example = "1")
    private Integer specialtyId;

    @Schema(description = "Nếu true, chỉ lấy các dịch vụ dùng chung (không gắn chuyên khoa)", example = "false")
    private Boolean generalOnly;

    @Schema(description = "Mức giá tối thiểu", example = "50000")
    private BigDecimal minPrice;

    @Schema(description = "Mức giá tối đa", example = "500000")
    private BigDecimal maxPrice;

    @Schema(description = "Thời gian khám dự kiến tối đa (phút)", example = "30")
    private Integer maxDuration;

    @Schema(description = "Lọc theo trạng thái hoạt động (Chỉ áp dụng cho ADMIN/STAFF)", example = "true")
    private Boolean isActive;
}
