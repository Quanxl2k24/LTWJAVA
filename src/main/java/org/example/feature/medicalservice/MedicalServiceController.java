package org.example.feature.medicalservice;

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
import org.example.feature.medicalservice.dto.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/medical-services")
@RequiredArgsConstructor
@Tag(name = "Medical Service Feature", description = "Các API quản lý danh mục dịch vụ khám, gói khám, giá dịch vụ và thời gian khám dự kiến")
public class MedicalServiceController {

    private final MedicalServiceService medicalServiceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Tạo mới dịch vụ khám (Admin)",
            description = "Dành riêng cho Quản trị viên. Tạo dịch vụ khám gắn với chuyên khoa cụ thể hoặc để specialtyId=null cho dịch vụ dùng chung/tổng quát."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo mới dịch vụ khám thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Chuyên khoa tạm ngưng hoạt động"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy chuyên khoa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tên dịch vụ bị trùng trong cùng chuyên khoa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dữ liệu vi phạm validation")
    })
    public ApiResponse<MedicalServiceResponse> create(@Valid @RequestBody CreateMedicalServiceRequest request) {
        MedicalServiceResponse response = medicalServiceService.create(request);
        return ApiResponse.success("Tạo dịch vụ khám thành công", response);
    }

    @GetMapping
    @Operation(
            summary = "Lấy danh sách dịch vụ khám (Tìm kiếm, lọc & phân trang)",
            description = "Public / Authenticated. Hỗ trợ tìm theo tên, lọc theo chuyên khoa, dịch vụ dùng chung (generalOnly), khoảng giá, thời gian khám tối đa. Với PATIENT/DOCTOR/Public tự động chỉ trả về dịch vụ đang mở và thuộc chuyên khoa đang mở (hoặc dịch vụ chung)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách dịch vụ khám thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bộ lọc xung đột hoặc khoảng giá không hợp lệ")
    })
    public ApiResponse<PageResponse<MedicalServiceResponse>> getAll(
            @Parameter(description = "Tìm gần đúng theo tên dịch vụ", example = "Siêu âm")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Lọc theo ID chuyên khoa cụ thể", example = "1")
            @RequestParam(required = false) Integer specialtyId,

            @Parameter(description = "Nếu true, chỉ lấy các dịch vụ dùng chung (specialty_id IS NULL)", example = "false")
            @RequestParam(required = false) Boolean generalOnly,

            @Parameter(description = "Giá tối thiểu", example = "50000")
            @RequestParam(required = false) BigDecimal minPrice,

            @Parameter(description = "Giá tối đa", example = "500000")
            @RequestParam(required = false) BigDecimal maxPrice,

            @Parameter(description = "Thời gian khám tối đa (phút)", example = "30")
            @RequestParam(required = false) Integer maxDuration,

            @Parameter(description = "Lọc trạng thái hoạt động (Chỉ áp dụng cho ADMIN/STAFF)", example = "true")
            @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "Số thứ tự trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số lượng bản ghi mỗi trang", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sắp xếp (VD: price,asc hoặc name,desc)", example = "id,asc")
            @RequestParam(defaultValue = "id,asc") String sort,

            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        MedicalServiceFilterRequest filter = MedicalServiceFilterRequest.builder()
                .keyword(keyword)
                .specialtyId(specialtyId)
                .generalOnly(generalOnly)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .maxDuration(maxDuration)
                .isActive(isActive)
                .build();

        Pageable pageable = PageRequest.of(Math.max(page, 0), size, parseSort(sort));
        PageResponse<MedicalServiceResponse> response = medicalServiceService.getAll(filter, pageable, currentUser);
        return ApiResponse.success("Lấy danh sách dịch vụ khám thành công", response);
    }

    @GetMapping("/{id:\\d+}")
    @Operation(
            summary = "Xem chi tiết dịch vụ khám theo ID",
            description = "Public / Authenticated. Trả về thông tin chi tiết dịch vụ kèm chuyên khoa (hoặc null nếu là dịch vụ dùng chung)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy chi tiết dịch vụ thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy dịch vụ hoặc dịch vụ đang bị ẩn")
    })
    public ApiResponse<MedicalServiceResponse> getById(
            @Parameter(description = "ID dịch vụ khám", example = "1", required = true)
            @PathVariable Integer id,
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        MedicalServiceResponse response = medicalServiceService.getById(id, currentUser);
        return ApiResponse.success("Lấy chi tiết dịch vụ khám thành công", response);
    }

    @PutMapping("/{id:\\d+}")
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Cập nhật dịch vụ khám (Admin)",
            description = "Dành riêng cho Quản trị viên. Cập nhật tên, giá, thời gian khám, chuyên khoa (hoặc đặt specialtyId=null để chuyển thành dịch vụ dùng chung)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật dịch vụ khám thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Chuyên khoa tạm ngưng hoạt động"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy dịch vụ hoặc chuyên khoa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tên dịch vụ bị trùng trong phạm vi chuyên khoa")
    })
    public ApiResponse<MedicalServiceResponse> update(
            @Parameter(description = "ID dịch vụ khám cần cập nhật", example = "1", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody UpdateMedicalServiceRequest request
    ) {
        MedicalServiceResponse response = medicalServiceService.update(id, request);
        return ApiResponse.success("Cập nhật dịch vụ khám thành công", response);
    }

    @RequestMapping(value = "/{id:\\d+}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @RequireRole({RoleConstant.ADMIN, RoleConstant.STAFF})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Bật / Tắt trạng thái dịch vụ khám (Admin/Staff) [Hỗ trợ PATCH/PUT]",
            description = "Cho phép ADMIN hoặc STAFF bật/tắt trạng thái hoạt động của dịch vụ. Không cho phép bật lại nếu chuyên khoa cha đang tạm ngưng."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật trạng thái dịch vụ thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Không thể bật dịch vụ do chuyên khoa cha đang bị tắt"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy dịch vụ khám")
    })
    public ApiResponse<MedicalServiceResponse> updateStatus(
            @Parameter(description = "ID dịch vụ khám", example = "1", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody UpdateMedicalServiceStatusRequest request
    ) {
        MedicalServiceResponse response = medicalServiceService.updateStatus(id, request);
        String action = Boolean.TRUE.equals(request.getIsActive()) ? "Mở" : "Tắt";
        return ApiResponse.success(action + " dịch vụ khám thành công", response);
    }

    private Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "id");
        }
        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim()))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }
}
