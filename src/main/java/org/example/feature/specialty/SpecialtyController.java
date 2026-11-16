package org.example.feature.specialty;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.api.ApiResponse;
import org.example.common.api.PageResponse;
import org.example.common.security.RequireRole;
import org.example.common.security.RoleConstant;
import org.example.feature.specialty.dto.SpecialtyCreateRequest;
import org.example.feature.specialty.dto.SpecialtyResponse;
import org.example.feature.specialty.dto.SpecialtyUpdateRequest;
import org.example.feature.specialty.dto.UpdateSpecialtyStatusRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
@RequiredArgsConstructor
@Tag(name = "Specialty Feature", description = "Các API quản lý danh mục chuyên khoa khám bệnh, phục vụ đặt lịch khám và tra cứu thông tin khoa phòng")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    // =========================================================================
    // 1. NHÓM API CÔNG KHAI (PUBLIC CHO BỆNH NHÂN & KHÁCH HÀNG)
    // =========================================================================

    @GetMapping("/active")
    @Operation(
            summary = "Lấy tất cả chuyên khoa đang hoạt động (Dropdown)",
            description = "API công khai (không cần đăng nhập). Trả về toàn bộ danh mục các chuyên khoa có trạng thái isActive=true, sắp xếp theo tên từ A-Z. Phục vụ hiển thị dropdown chọn khoa khi đặt lịch hẹn."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh mục chuyên khoa hoạt động thành công")
    })
    public ApiResponse<List<SpecialtyResponse>> getActiveSpecialties() {
        List<SpecialtyResponse> list = specialtyService.getActiveSpecialties();
        return ApiResponse.success("Lấy danh sách chuyên khoa đang hoạt động thành công", list);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Xem thông tin chi tiết chuyên khoa theo ID",
            description = "API công khai. Lấy thông tin chi tiết của một chuyên khoa theo mã ID định danh."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy chi tiết chuyên khoa thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy chuyên khoa")
    })
    public ApiResponse<SpecialtyResponse> getSpecialtyById(
            @Parameter(description = "ID của chuyên khoa cần xem", example = "1", required = true)
            @PathVariable Integer id
    ) {
        SpecialtyResponse response = specialtyService.getSpecialtyById(id);
        return ApiResponse.success("Lấy thông tin chuyên khoa thành công", response);
    }

    @GetMapping("/slug/{slug}")
    @Operation(
            summary = "Xem chi tiết chuyên khoa theo đường dẫn Slug",
            description = "API công khai. Cho phép tra cứu thông tin chuyên khoa thông qua chuỗi slug thân thiện với SEO (ví dụ: 'khoa-tim-mach', 'khoa-nhi')."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thông tin chuyên khoa thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy chuyên khoa với slug chỉ định")
    })
    public ApiResponse<SpecialtyResponse> getSpecialtyBySlug(
            @Parameter(description = "Chuỗi slug của chuyên khoa", example = "khoa-tim-mach", required = true)
            @PathVariable String slug
    ) {
        SpecialtyResponse response = specialtyService.getSpecialtyBySlug(slug);
        return ApiResponse.success("Lấy thông tin chuyên khoa theo slug thành công", response);
    }

    // =========================================================================
    // 2. NHÓM API QUẢN TRỊ (ADMIN & STAFF)
    // =========================================================================

    @GetMapping
    @RequireRole({RoleConstant.ADMIN, RoleConstant.STAFF})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Quản lý danh sách chuyên khoa (Phân trang, Tìm kiếm & Lọc)",
            description = "Dành cho Quản trị viên và Nhân viên. Tìm kiếm chuyên khoa theo từ khóa (tên, slug, mô tả), lọc theo trạng thái hoạt động và phân trang."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách chuyên khoa thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực danh tính"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập (Yêu cầu ADMIN hoặc STAFF)")
    })
    public ApiResponse<PageResponse<SpecialtyResponse>> getSpecialties(
            @Parameter(description = "Từ khóa tìm kiếm (tên, slug hoặc mô tả chuyên khoa)", example = "Nhi")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Lọc theo trạng thái hoạt động (true: Đang hoạt động, false: Đã tắt)", example = "true")
            @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "Số thứ tự trang (bắt đầu từ 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Số lượng bản ghi trên một trang", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Trường dùng để sắp xếp", example = "id")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Hướng sắp xếp (asc hoặc desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PageResponse<SpecialtyResponse> response = specialtyService.getSpecialties(keyword, isActive, page, size, sortBy, sortDir);
        return ApiResponse.success("Lấy danh sách chuyên khoa thành công", response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Tạo mới chuyên khoa (Admin)",
            description = "Dành riêng cho Quản trị viên. Tạo chuyên khoa mới. Nếu trường 'slug' để trống, hệ thống sẽ tự động chuyển đổi từ 'name' sang slug không dấu chuẩn SEO."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo mới chuyên khoa thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tên chuyên khoa hoặc slug đã tồn tại trên hệ thống"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dữ liệu vi phạm quy tắc validation")
    })
    public ApiResponse<SpecialtyResponse> createSpecialty(@Valid @RequestBody SpecialtyCreateRequest request) {
        SpecialtyResponse response = specialtyService.createSpecialty(request);
        return ApiResponse.success("Tạo mới chuyên khoa thành công", response);
    }

    @PutMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Cập nhật chuyên khoa (Admin)",
            description = "Dành riêng cho Quản trị viên. Cập nhật tên, slug, mô tả hoặc trạng thái của chuyên khoa theo ID."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật chuyên khoa thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy chuyên khoa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tên hoặc slug bị trùng lặp với chuyên khoa khác")
    })
    public ApiResponse<SpecialtyResponse> updateSpecialty(
            @Parameter(description = "ID của chuyên khoa cần cập nhật", example = "1", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody SpecialtyUpdateRequest request
    ) {
        SpecialtyResponse response = specialtyService.updateSpecialty(id, request);
        return ApiResponse.success("Cập nhật thông tin chuyên khoa thành công", response);
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Bật / Tắt trạng thái hoạt động của chuyên khoa (Admin) [Hỗ trợ PATCH/PUT]",
            description = "Dành riêng cho Quản trị viên. Bật (true) hoặc Tắt/Ẩn (false) chuyên khoa. Khi tắt, chuyên khoa sẽ không còn xuất hiện trong danh sách đặt lịch hẹn của bệnh nhân."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy chuyên khoa")
    })
    public ApiResponse<SpecialtyResponse> updateSpecialtyStatus(
            @Parameter(description = "ID của chuyên khoa cần thay đổi trạng thái", example = "1", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody UpdateSpecialtyStatusRequest request
    ) {
        SpecialtyResponse response = specialtyService.updateSpecialtyStatus(id, request);
        String action = Boolean.TRUE.equals(request.getIsActive()) ? "Bật" : "Tắt";
        return ApiResponse.success(action + " hoạt động chuyên khoa thành công", response);
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Xóa chuyên khoa (Admin)",
            description = "Dành riêng cho Quản trị viên. Chỉ xóa được chuyên khoa khi chưa có bác sĩ nào thuộc khoa này. Nếu đã có bác sĩ, hệ thống sẽ từ chối và yêu cầu tắt hoạt động thay vì xóa."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa chuyên khoa thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy chuyên khoa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xóa do đang có bác sĩ trực thuộc")
    })
    public ApiResponse<Void> deleteSpecialty(
            @Parameter(description = "ID của chuyên khoa cần xóa", example = "3", required = true)
            @PathVariable Integer id
    ) {
        specialtyService.deleteSpecialty(id);
        return ApiResponse.success("Xóa chuyên khoa thành công", null);
    }
}

