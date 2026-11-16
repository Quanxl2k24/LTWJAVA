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
@Schema(description = "Dữ liệu yêu cầu tạo mới người dùng từ trang quản trị (Admin)")
public class UserCreateRequest {

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])[0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam (10 chữ số)")
    @Schema(description = "Số điện thoại duy nhất dùng để đăng nhập", example = "0912345679", requiredMode = Schema.RequiredMode.REQUIRED)
    private String phone;

    @Email(message = "Email không đúng định dạng chuẩn")
    @Size(max = 100, message = "Email không được vượt quá 100 ký tự")
    @Schema(description = "Địa chỉ email (tùy chọn, duy nhất)", example = "bacsi.tran@hospital.com")
    private String email;

    @Size(min = 6, max = 50, message = "Mật khẩu khởi tạo phải từ 6 đến 50 ký tự")
    @Schema(description = "Mật khẩu khởi tạo (Nếu bỏ trống, hệ thống sẽ gán mật khẩu mặc định: 'Abc@123456')", example = "Hospital@2026")
    private String password;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ và tên phải có độ dài từ 2 đến 100 ký tự")
    @Schema(description = "Họ và tên đầy đủ", example = "Trần Thị Bích", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @NotNull(message = "ID vai trò không được để trống")
    @Schema(description = "ID vai trò (1: ADMIN, 2: DOCTOR, 3: STAFF, 4: PATIENT)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer roleId;

    @Pattern(regexp = "^(male|female|other)$", message = "Giới tính chỉ nhận một trong các giá trị: male, female, other")
    @Schema(description = "Giới tính (male: Nam, female: Nữ, other: Khác)", example = "female", allowableValues = {"male", "female", "other"})
    private String gender;

    @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
    @Schema(description = "Ngày tháng năm sinh (định dạng YYYY-MM-DD)", example = "1990-08-15")
    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    @Schema(description = "Địa chỉ liên hệ", example = "Phòng 402, Toà nhà Y khoa, Hà Nội")
    private String address;

    @Schema(description = "Kích hoạt tài khoản ngay lập tức (mặc định: true)", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean isActive = true;
}

