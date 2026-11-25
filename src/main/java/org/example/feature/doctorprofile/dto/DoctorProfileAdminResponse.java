package org.example.feature.doctorprofile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin hồ sơ bác sĩ đầy đủ dành cho Quản trị viên và Nhân viên")
public class DoctorProfileAdminResponse {

    @Schema(description = "ID hồ sơ bác sĩ", example = "1")
    private Long id;

    @Schema(description = "Học hàm / học vị", example = "ThS. BS CKII")
    private String title;

    @Schema(description = "Số năm kinh nghiệm", example = "10")
    private Integer experienceYears;

    @Schema(description = "Tiểu sử chuyên môn", example = "Chuyên gia đầu ngành tim mạch.")
    private String biography;

    @Schema(description = "Phí tư vấn / khám bệnh", example = "300000.00")
    private BigDecimal consultationFee;

    @Schema(description = "Trạng thái sẵn sàng nhận khám", example = "true")
    private Boolean isAvailable;

    @Schema(description = "Thông tin tài khoản bác sĩ đầy đủ cho quản trị")
    private UserAdminSummary user;

    @Schema(description = "Thông tin chuyên khoa")
    private SpecialtyAdminSummary specialty;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Thông tin người dùng dành cho quản trị")
    public static class UserAdminSummary {
        @Schema(description = "ID tài khoản", example = "2")
        private Long id;

        @Schema(description = "Họ và tên bác sĩ", example = "Nguyễn Văn An")
        private String fullName;

        @Schema(description = "Giới tính", example = "male")
        private String gender;

        @Schema(description = "Số điện thoại", example = "0988776655")
        private String phone;

        @Schema(description = "Email", example = "doctor.an@hospital.com")
        private String email;

        @Schema(description = "Trạng thái hoạt động tài khoản", example = "true")
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Thông tin chuyên khoa dành cho quản trị")
    public static class SpecialtyAdminSummary {
        @Schema(description = "ID chuyên khoa", example = "1")
        private Integer id;

        @Schema(description = "Tên chuyên khoa", example = "Khoa Tim Mạch")
        private String name;

        @Schema(description = "Slug chuyên khoa", example = "khoa-tim-mach")
        private String slug;

        @Schema(description = "Trạng thái hoạt động chuyên khoa", example = "true")
        private Boolean isActive;
    }
}
