package org.example.feature.doctorprofile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu bác sĩ tự cập nhật tiểu sử chuyên môn")
public class UpdateMyDoctorProfileRequest {

    @Size(max = 5000, message = "Tiểu sử chuyên môn không được vượt quá 5000 ký tự")
    @Schema(description = "Nội dung tiểu sử chuyên môn mới", example = "Tốt nghiệp ĐH Y Hà Nội, tu nghiệp tại Pháp chuyên ngành Tim mạch can thiệp.")
    private String biography;
}
