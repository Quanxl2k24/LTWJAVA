package org.example.feature.patientProfiles.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin hồ sơ bệnh nhân")
public class PatientProfileResponse {

    @Schema(description = "ID hồ sơ bệnh nhân", example = "1")
    private Long id;

    @Schema(description = "ID tài khoản người dùng sở hữu hồ sơ", example = "10")
    private Long userId;

    @Schema(description = "Họ và tên bệnh nhân", example = "Nguyễn Văn Bệnh Nhân")
    private String fullName;

    @Schema(description = "Số điện thoại liên hệ", example = "0912345678")
    private String phone;

    @Schema(description = "Số CCCD hoặc mã thẻ BHYT", example = "001201009999")
    private String identityCard;

    @Schema(description = "Ngày tháng năm sinh", example = "1995-10-20")
    private LocalDate dateOfBirth;

    @Schema(description = "Giới tính (male, female, other)", example = "male")
    private String gender;

    @Schema(description = "Địa chỉ cư trú", example = "Số 10 Phố Huế, Hà Nội")
    private String address;

    @Schema(description = "Tiền sử bệnh án, dị ứng thuốc", example = "Dị ứng Penicillin, tiền sử đau dạ dày")
    private String medicalHistory;

    @Schema(description = "Thời điểm tạo hồ sơ", example = "2026-11-20T08:30:00Z")
    private Instant createdAt;
}
