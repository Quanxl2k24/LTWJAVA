package org.example.feature.doctorprofile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu tạo mới hồ sơ bác sĩ")
public class CreateDoctorProfileRequest {

    @NotNull(message = "ID người dùng không được để trống")
    @Positive(message = "ID người dùng phải là số dương")
    @Schema(description = "ID tài khoản bác sĩ (phải có vai trò DOCTOR và đang hoạt động)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "ID chuyên khoa không được để trống")
    @Positive(message = "ID chuyên khoa phải là số dương")
    @Schema(description = "ID chuyên khoa trực thuộc", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer specialtyId;

    @Size(max = 50, message = "Học hàm / học vị không được vượt quá 50 ký tự")
    @Schema(description = "Học hàm, học vị hoặc chức danh chuyên môn", example = "ThS. BS CKII")
    private String title;

    @Min(value = 0, message = "Số năm kinh nghiệm không được nhỏ hơn 0")
    @Max(value = 70, message = "Số năm kinh nghiệm không được vượt quá 70")
    @Schema(description = "Số năm kinh nghiệm hành nghề", example = "10")
    @Builder.Default
    private Integer experienceYears = 0;

    @Size(max = 5000, message = "Tiểu sử chuyên môn không được vượt quá 5000 ký tự")
    @Schema(description = "Tiểu sử, quá trình công tác và thành tựu chuyên môn", example = "Tốt nghiệp Đại học Y Hà Nội, hơn 10 năm kinh nghiệm điều trị bệnh lý tim mạch.")
    private String biography;

    @NotNull(message = "Phí khám bệnh không được để trống")
    @DecimalMin(value = "0.00", message = "Phí khám bệnh không được nhỏ hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Phí khám bệnh tối đa 10 chữ số phần nguyên và 2 chữ số thập phân")
    @Schema(description = "Phí tư vấn / khám bệnh (VNĐ)", example = "300000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal consultationFee;

    @Schema(description = "Trạng thái sẵn sàng nhận lịch khám (mặc định: true)", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isAvailable = true;
}
