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
@Schema(description = "Dữ liệu yêu cầu cập nhật chuyên khoa khám bệnh")
public class SpecialtyUpdateRequest {

    @NotBlank(message = "Tên chuyên khoa không được để trống")
    @Size(min = 2, max = 100, message = "Tên chuyên khoa phải có độ dài từ 2 đến 100 ký tự")
    @Schema(description = "Tên chuyên khoa khám bệnh", example = "Khoa Nhi & Sơ Sinh", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 120, message = "Slug không được vượt quá 120 ký tự")
    @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug chỉ chứa chữ thường không dấu, số và dấu gạch nối (ví dụ: khoa-nhi-so-sinh)")
    @Schema(description = "Đường dẫn URL thân thiện (Tùy chọn. Nếu để trống hệ thống sẽ tự động tạo từ tên)", example = "khoa-nhi-so-sinh")
    private String slug;

    @Schema(description = "Mô tả chi tiết chuyên khoa", example = "Chăm sóc và điều trị chuyên sâu cho trẻ em và trẻ sơ sinh.")
    private String description;

    @Schema(description = "Trạng thái hoạt động của chuyên khoa", example = "true")
    private Boolean isActive;
}

