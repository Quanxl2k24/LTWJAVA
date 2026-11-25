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
@Schema(description = "Dữ liệu yêu cầu cập nhật hồ sơ bác sĩ bởi Quản trị viên")
public class UpdateDoctorProfileRequest {

    @NotNull(message = "ID chuyên khoa không được để trống")
    @Positive(message = "ID chuyên khoa phải là số dương")
    @Schema(description = "ID chuyên khoa trực thuộc", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer specialtyId;

    @Size(max = 50, message = "Học hàm / học vị không được vượt quá 50 ký tự")
    @Schema(description = "Học hàm, học vị hoặc chức danh chuyên môn", example = "PGS. TS. BS")
    private String title;

    @Min(value = 0, message = "Số năm kinh nghiệm không được nhỏ hơn 0")
    @Max(value = 70, message = "Số năm kinh nghiệm không được vượt quá 70")
    @Schema(description = "Số năm kinh nghiệm hành nghề", example = "15")
    private Integer experienceYears;

    @Size(max = 5000, message = "Tiểu sử chuyên môn không được vượt quá 5000 ký tự")
    @Schema(description = "Tiểu sử, quá trình công tác và thành tựu chuyên môn", example = "Chuyên gia đầu ngành về Nội Tim mạch.")
    private String biography;

    @NotNull(message = "Phí khám bệnh không được để trống")
    @DecimalMin(value = "0.00", message = "Phí khám bệnh không được nhỏ hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Phí khám bệnh tối đa 10 chữ số phần nguyên và 2 chữ số thập phân")
    @Schema(description = "Phí tư vấn / khám bệnh (VNĐ)", example = "500000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal consultationFee;

    @Schema(description = "Trạng thái sẵn sàng nhận lịch khám", example = "true")
    private Boolean isAvailable;
}
