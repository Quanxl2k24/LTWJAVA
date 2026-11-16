package org.example.feature.specialty.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu tạo mới chuyên khoa khám bệnh")
public class SpecialtyCreateRequest {

    @NotBlank(message = "Tên chuyên khoa không được để trống")
    @Size(min = 2, max = 100, message = "Tên chuyên khoa phải có độ dài từ 2 đến 100 ký tự")
    @Schema(description = "Tên chuyên khoa khám bệnh", example = "Khoa Nhi", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 120, message = "Slug không được vượt quá 120 ký tự")
    @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug chỉ chứa chữ thường không dấu, số và dấu gạch nối (ví dụ: khoa-nhi)")
    @Schema(description = "Đường dẫn URL thân thiện (Tùy chọn. Nếu để trống hệ thống sẽ tự động tạo từ tên chuyên khoa)", example = "khoa-nhi")
    private String slug;

    @Schema(description = "Mô tả chi tiết về chuyên khoa", example = "Khám, tư vấn và điều trị toàn diện các bệnh lý ở trẻ sơ sinh và trẻ em.")
    private String description;

    @Schema(description = "Trạng thái kích hoạt chuyên khoa (mặc định: true)", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isActive = true;
}

