package org.example.feature.doctorprofile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.api.ApiResponse;
import org.example.common.api.PageResponse;
import org.example.common.security.CurrentUser;
import org.example.common.security.RequireRole;
import org.example.common.security.RoleConstant;
import org.example.common.security.UserPrincipal;
import org.example.feature.doctorprofile.dto.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/doctor-profiles")
@RequiredArgsConstructor
@Tag(name = "Doctor Profile Feature", description = "Các API quản lý hồ sơ chuyên môn bác sĩ, tra cứu công khai và điều chỉnh trạng thái nhận khám")
public class DoctorProfileController {

    private final DoctorProfileService doctorProfileService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Tạo mới hồ sơ bác sĩ (Admin)",
            description = "Dành riêng cho Quản trị viên. Gắn tài khoản có vai trò DOCTOR đang hoạt động vào một chuyên khoa đang mở."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo mới hồ sơ bác sĩ thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tài khoản không phải vai trò DOCTOR, bị khóa, hoặc chuyên khoa tạm ngưng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng hoặc chuyên khoa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Người dùng này đã có hồ sơ bác sĩ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dữ liệu đầu vào vi phạm validation")
    })
    public ApiResponse<DoctorProfileAdminResponse> create(@Valid @RequestBody CreateDoctorProfileRequest request) {
        DoctorProfileAdminResponse response = doctorProfileService.create(request);
        return ApiResponse.success("Tạo hồ sơ bác sĩ thành công", response);
    }

    @GetMapping
    @Operation(
            summary = "Tìm kiếm & lấy danh sách hồ sơ bác sĩ (Phân trang)",
            description = "Public / Authenticated. Nếu người gọi là ADMIN hoặc STAFF, trả về dữ liệu quản trị và cho phép lọc tự do theo isAvailable. Nếu là PATIENT, DOCTOR hoặc chưa đăng nhập, tự động lọc chỉ hiển thị bác sĩ đang hoạt động và ẩn thông tin liên hệ nhạy cảm."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách hồ sơ bác sĩ thành công")
    })
    public ApiResponse<PageResponse<?>> getAll(
            @Parameter(description = "Từ khóa tìm kiếm theo tên bác sĩ hoặc học hàm/học vị", example = "ThS")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Lọc theo ID chuyên khoa", example = "1")
            @RequestParam(required = false) Integer specialtyId,

            @Parameter(description = "Phí khám tối thiểu", example = "100000")
            @RequestParam(required = false) BigDecimal minFee,

            @Parameter(description = "Phí khám tối đa", example = "500000")
            @RequestParam(required = false) BigDecimal maxFee,

            @Parameter(description = "Số năm kinh nghiệm tối thiểu", example = "5")
            @RequestParam(required = false) Integer minExperience,

            @Parameter(description = "Trạng thái nhận khám (Chỉ có hiệu lực với ADMIN/STAFF)", example = "true")
            @RequestParam(required = false) Boolean isAvailable,

            @Parameter(description = "Số thứ tự trang (bắt đầu từ 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Số lượng bản ghi mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Trường sắp xếp", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Hướng sắp xếp (asc / desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir,

            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        DoctorProfileFilterRequest filter = DoctorProfileFilterRequest.builder()
                .keyword(keyword)
                .specialtyId(specialtyId)
                .minFee(minFee)
                .maxFee(maxFee)
                .minExperience(minExperience)
                .isAvailable(isAvailable)
                .build();

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, sort);

        PageResponse<?> response = doctorProfileService.getAll(filter, pageable, currentUser);
        return ApiResponse.success("Lấy danh sách hồ sơ bác sĩ thành công", response);
    }

    @GetMapping("/me")
    @RequireRole({RoleConstant.DOCTOR})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Bác sĩ xem hồ sơ chuyên môn của chính mình",
            description = "Dành riêng cho tài khoản vai trò DOCTOR. Trả về hồ sơ chi tiết của bác sĩ đang đăng nhập."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy hồ sơ thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không phải tài khoản DOCTOR"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tài khoản chưa được tạo hồ sơ bác sĩ")
    })
    public ApiResponse<DoctorProfileAdminResponse> getMyProfile(
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        DoctorProfileAdminResponse response = doctorProfileService.getMyProfile(currentUser.getId());
        return ApiResponse.success("Lấy hồ sơ bác sĩ cá nhân thành công", response);
    }

    @GetMapping("/{id:\\d+}")
    @Operation(
            summary = "Xem chi tiết hồ sơ bác sĩ theo ID",
            description = "Public / Authenticated. Khách hàng/Bệnh nhân chỉ xem được nếu bác sĩ đang hoạt động (isAvailable=true, user.isActive=true, specialty.isActive=true) và không lộ thông tin cá nhân nhạy cảm."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy chi tiết hồ sơ bác sĩ thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy hồ sơ bác sĩ hoặc hồ sơ đang bị ẩn")
    })
    public ApiResponse<Object> getById(
            @Parameter(description = "ID hồ sơ bác sĩ", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        Object response = doctorProfileService.getById(id, currentUser);
        return ApiResponse.success("Lấy chi tiết hồ sơ bác sĩ thành công", response);
    }

    @PutMapping("/{id:\\d+}")
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Cập nhật hồ sơ bác sĩ (Admin)",
            description = "Dành riêng cho Quản trị viên. Cập nhật chuyên khoa, học vị, kinh nghiệm, tiểu sử, phí khám và trạng thái nhận khám."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật hồ sơ bác sĩ thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Chuyên khoa mới tạm ngưng hoạt động"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy hồ sơ bác sĩ hoặc chuyên khoa")
    })
    public ApiResponse<DoctorProfileAdminResponse> update(
            @Parameter(description = "ID hồ sơ bác sĩ", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateDoctorProfileRequest request
    ) {
        DoctorProfileAdminResponse response = doctorProfileService.update(id, request);
        return ApiResponse.success("Cập nhật hồ sơ bác sĩ thành công", response);
    }

    @RequestMapping(value = "/me", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @RequireRole({RoleConstant.DOCTOR})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Bác sĩ tự cập nhật tiểu sử chuyên môn (Hỗ trợ PATCH/PUT)",
            description = "Dành riêng cho tài khoản DOCTOR đang đăng nhập. Chỉ cho phép tự chỉnh sửa nội dung tiểu sử chuyên môn (biography)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật tiểu sử thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy hồ sơ bác sĩ")
    })
    public ApiResponse<DoctorProfileAdminResponse> updateMyProfile(
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody UpdateMyDoctorProfileRequest request
    ) {
        DoctorProfileAdminResponse response = doctorProfileService.updateMyProfile(currentUser.getId(), request);
        return ApiResponse.success("Cập nhật tiểu sử chuyên môn thành công", response);
    }

    @RequestMapping(value = "/{id:\\d+}/availability", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @RequireRole({RoleConstant.ADMIN, RoleConstant.STAFF, RoleConstant.DOCTOR})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Bật / Tắt trạng thái nhận khám của bác sĩ (Hỗ trợ PATCH/PUT)",
            description = "Cho phép ADMIN, STAFF hoặc chính DOCTOR sở hữu hồ sơ thay đổi trạng thái sẵn sàng nhận khám (isAvailable)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật trạng thái nhận khám thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Bác sĩ cố tình sửa trạng thái hồ sơ của bác sĩ khác"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy hồ sơ bác sĩ")
    })
    public ApiResponse<DoctorProfileAdminResponse> updateAvailability(
            @Parameter(description = "ID hồ sơ bác sĩ", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateDoctorAvailabilityRequest request,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        DoctorProfileAdminResponse response = doctorProfileService.updateAvailability(id, request, currentUser);
        return ApiResponse.success("Cập nhật trạng thái nhận khám thành công", response);
    }
}
