package org.example.feature.doctorprofile.dto;

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
@Schema(description = "Bộ lọc tìm kiếm danh sách hồ sơ bác sĩ")
public class DoctorProfileFilterRequest {

    @Schema(description = "Từ khóa tìm kiếm theo họ tên bác sĩ hoặc học hàm/học vị", example = "ThS")
    private String keyword;

    @Schema(description = "Lọc theo ID chuyên khoa", example = "1")
    private Integer specialtyId;

    @Schema(description = "Mức phí khám tối thiểu", example = "100000")
    private BigDecimal minFee;

    @Schema(description = "Mức phí khám tối đa", example = "500000")
    private BigDecimal maxFee;

    @Schema(description = "Số năm kinh nghiệm tối thiểu", example = "5")
    private Integer minExperience;

    @Schema(description = "Lọc theo trạng thái nhận khám (Chỉ áp dụng khi ADMIN/STAFF gọi)", example = "true")
    private Boolean isAvailable;
}
