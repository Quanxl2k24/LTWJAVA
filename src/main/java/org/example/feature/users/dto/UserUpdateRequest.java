package org.example.feature.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu cập nhật thông tin người dùng bởi Quản trị viên")
public class UserUpdateRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải có độ dài từ 2 đến 100 ký tự")
    @Schema(description = "Họ và tên đầy đủ", example = "Bác sĩ Trần Thị Bích", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @Email(message = "Email không đúng định dạng chuẩn")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    @Schema(description = "Địa chỉ email liên hệ", example = "bacsi.tran.update@hospital.com")
    private String email;

    @NotNull(message = "ID vai trò không được để trống")
    @Schema(description = "ID vai trò trong hệ thống (1: ADMIN, 2: DOCTOR, 3: STAFF, 4: PATIENT)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer roleId;

    @Pattern(regexp = "^(male|female|other)$", message = "Giới tính chỉ nhận một trong các giá trị: male, female, other")
    @Schema(description = "Giới tính (male: Nam, female: Nữ, other: Khác)", example = "female", allowableValues = {"male", "female", "other"})
    private String gender;

    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    @Schema(description = "Ngày tháng năm sinh (định dạng YYYY-MM-DD)", example = "1990-08-15")
    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    @Schema(description = "Địa chỉ liên hệ", example = "Tầng 5, Bệnh viện Đa khoa, Hà Nội")
    private String address;

    @Schema(description = "Trạng thái hoạt động tài khoản", example = "true")
    private Boolean isActive;
}

