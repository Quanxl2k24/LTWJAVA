package org.example.feature.medicalservice.dto;

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
@Schema(description = "Dữ liệu yêu cầu tạo mới dịch vụ khám / gói khám")
public class CreateMedicalServiceRequest {

    @Positive(message = "ID chuyên khoa phải là số dương")
    @Schema(description = "ID chuyên khoa phụ trách (để null nếu là dịch vụ tổng quát / dùng chung)", example = "1")
    private Integer specialtyId;

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 150, message = "Tên dịch vụ không được vượt quá 150 ký tự")
    @Schema(description = "Tên dịch vụ khám", example = "Siêu âm tim Doppler màu", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "Giá dịch vụ không được để trống")
    @DecimalMin(value = "0.00", message = "Giá dịch vụ không được nhỏ hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá dịch vụ tối đa 10 chữ số phần nguyên và 2 chữ số thập phân")
    @Schema(description = "Giá dịch vụ (VNĐ)", example = "250000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Min(value = 5, message = "Thời gian khám dự kiến tối thiểu là 5 phút")
    @Max(value = 480, message = "Thời gian khám dự kiến tối đa là 480 phút")
    @Schema(description = "Thời gian khám dự kiến tính bằng phút (mặc định: 15)", example = "20", defaultValue = "15")
    @Builder.Default
    private Integer estimatedDurationMin = 15;

    @Size(max = 5000, message = "Mô tả dịch vụ không được vượt quá 5000 ký tự")
    @Schema(description = "Mô tả chi tiết và lưu ý chuẩn bị trước khi khám", example = "Không cần nhịn ăn trước khi siêu âm.")
    private String description;

    @Schema(description = "Trạng thái mở cung cấp dịch vụ (mặc định: true)", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isActive = true;
}
