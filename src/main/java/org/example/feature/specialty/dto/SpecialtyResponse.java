package org.example.feature.specialty.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết chuyên khoa khám bệnh")
public class SpecialtyResponse {

    @Schema(description = "ID định danh chuyên khoa", example = "1")
    private Integer id;

    @Schema(description = "Tên chuyên khoa khám bệnh", example = "Khoa Tim Mạch")
    private String name;

    @Schema(description = "Đường dẫn URL thân thiện (Slug duy nhất)", example = "khoa-tim-mach")
    private String slug;

    @Schema(description = "Mô tả chi tiết chức năng, nhiệm vụ và các bệnh lý tiếp nhận", example = "Chẩn đoán và điều trị chuyên sâu các bệnh lý về tim mạch, huyết áp, mạch vành.")
    private String description;

    @Schema(description = "Trạng thái hoạt động (true: Đang hoạt động, false: Đã tắt / tạm ngưng)", example = "true")
    private Boolean isActive;
}

