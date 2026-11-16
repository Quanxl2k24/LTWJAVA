package org.example.feature.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu người dùng tự cập nhật thông tin cá nhân")
public class UserProfileUpdateRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải có độ dài từ 2 đến 100 ký tự")
    @Schema(description = "Họ và tên đầy đủ của người dùng", example = "Nguyễn Văn An", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @Pattern(regexp = "^(male|female|other)$", message = "Giới tính chỉ nhận một trong các giá trị: male, female, other")
    @Schema(description = "Giới tính (male: Nam, female: Nữ, other: Khác)", example = "male", allowableValues = {"male", "female", "other"})
    private String gender;

    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    @Schema(description = "Ngày tháng năm sinh (định dạng YYYY-MM-DD)", example = "1995-12-10")
    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    @Schema(description = "Địa chỉ cư trú", example = "Số 45 Ngõ 10 Đường Hoàng Quốc Việt, Cầu Giấy, Hà Nội")
    private String address;
}

