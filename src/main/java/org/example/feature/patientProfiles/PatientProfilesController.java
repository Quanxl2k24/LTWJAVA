package org.example.feature.patientProfiles;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.common.api.ApiResponse;
import org.example.common.security.CurrentUser;
import org.example.common.security.RequireRole;
import org.example.common.security.UserPrincipal;
import org.example.feature.patientProfiles.dto.PatientProfileResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patient-profile")
@RequiredArgsConstructor
@Tag(name = "Patient Profile Feature", description = "Các API quản lý hồ sơ bệnh nhân")
@SecurityRequirement(name = "BearerAuth")
public class PatientProfilesController {

    private final PatientProfilesService patientProfilesService;

    @GetMapping("/me")
    @RequireRole
    @Operation(
            summary = "Lấy danh sách hồ sơ bệnh nhân của người dùng hiện tại",
            description = "Lấy tất cả các hồ sơ bệnh nhân (bản thân, người thân) được liên kết với tài khoản đang đăng nhập"
    )
    public ApiResponse<List<PatientProfileResponse>> getMyProfiles(@CurrentUser UserPrincipal currentUser) {
        List<PatientProfileResponse> profiles = patientProfilesService.getMyPatientProfiles(currentUser.getId());
        return ApiResponse.success("Lấy danh sách hồ sơ bệnh nhân thành công", profiles);
    }
}
