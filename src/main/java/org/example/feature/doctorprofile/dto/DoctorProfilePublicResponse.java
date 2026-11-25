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
@Schema(description = "Thông tin hồ sơ bác sĩ hiển thị công khai (ẩn thông tin liên hệ nhạy cảm)")
public class DoctorProfilePublicResponse {

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

    @Schema(description = "Thông tin tài khoản bác sĩ tối giản")
    private UserPublicSummary user;

    @Schema(description = "Thông tin chuyên khoa")
    private SpecialtySummary specialty;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Thông tin người dùng công khai")
    public static class UserPublicSummary {
        @Schema(description = "ID tài khoản", example = "2")
        private Long id;

        @Schema(description = "Họ và tên bác sĩ", example = "Nguyễn Văn An")
        private String fullName;

        @Schema(description = "Giới tính", example = "male")
        private String gender;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Thông tin tóm tắt chuyên khoa")
    public static class SpecialtySummary {
        @Schema(description = "ID chuyên khoa", example = "1")
        private Integer id;

        @Schema(description = "Tên chuyên khoa", example = "Khoa Tim Mạch")
        private String name;

        @Schema(description = "Slug chuyên khoa", example = "khoa-tim-mach")
        private String slug;
    }
}
