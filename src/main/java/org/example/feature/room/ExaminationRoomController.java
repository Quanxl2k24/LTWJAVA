package org.example.feature.room;

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
import org.example.feature.room.dto.ExaminationRoomCreateRequest;
import org.example.feature.room.dto.ExaminationRoomResponse;
import org.example.feature.room.dto.ExaminationRoomUpdateRequest;
import org.example.feature.room.dto.UpdateRoomStatusRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/examination-rooms")
@RequiredArgsConstructor
@Tag(name = "Examination Room Feature", description = "Các API quản lý phòng khám thực tế tại bệnh viện, phục vụ xếp lịch làm việc của bác sĩ và điều phối bệnh nhân")
@SecurityRequirement(name = "BearerAuth")
public class ExaminationRoomController {

    private final ExaminationRoomService examinationRoomService;

    // =========================================================================
    // 1. NHÓM TRA CỨU & DANH MỤC DROPDOWN (STAFF, DOCTOR, ADMIN)
    // =========================================================================

    @GetMapping("/active")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.STAFF, RoleConstant.DOCTOR})
    @Operation(
            summary = "Lấy tất cả phòng khám đang hoạt động (Dropdown)",
            description = "Trả về danh sách các phòng khám có trạng thái sẵn sàng (isActive=true), sắp xếp theo số phòng. Phục vụ việc xếp ca trực cho bác sĩ và chọn phòng tiếp đón bệnh nhân."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách phòng khám hoạt động thành công")
    })
    public ApiResponse<List<ExaminationRoomResponse>> getActiveRooms() {
        List<ExaminationRoomResponse> list = examinationRoomService.getActiveRooms();
        return ApiResponse.success("Lấy danh sách phòng khám đang hoạt động thành công", list);
    }

    @GetMapping("/buildings")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.STAFF})
    @Operation(
            summary = "Lấy danh sách các tòa nhà / khu vực phòng khám",
            description = "Trả về danh sách tên các tòa nhà hoặc khu nhà khám bệnh hiện có trong hệ thống để Frontend làm bộ lọc Dropdown."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách tòa nhà thành công")
    })
    public ApiResponse<List<String>> getDistinctBuildings() {
        List<String> buildings = examinationRoomService.getDistinctBuildings();
        return ApiResponse.success("Lấy danh sách khu nhà thành công", buildings);
    }

    @GetMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.STAFF})
    @Operation(
            summary = "Xem chi tiết thông tin phòng khám theo ID",
            description = "Lấy thông tin chi tiết một phòng khám bao gồm số phòng, tòa nhà và trạng thái."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy thông tin phòng khám thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng khám với ID chỉ định")
    })
    public ApiResponse<ExaminationRoomResponse> getRoomById(
            @Parameter(description = "ID phòng khám", example = "1", required = true)
            @PathVariable Integer id
    ) {
        ExaminationRoomResponse response = examinationRoomService.getRoomById(id);
        return ApiResponse.success("Lấy thông tin phòng khám thành công", response);
    }

    // =========================================================================
    // 2. NHÓM QUẢN TRỊ VIÊN & ĐIỀU PHỐI (ADMIN & STAFF)
    // =========================================================================

    @GetMapping
    @RequireRole({RoleConstant.ADMIN, RoleConstant.STAFF})
    @Operation(
            summary = "Quản lý danh sách phòng khám (Phân trang, Tìm kiếm & Lọc)",
            description = "Tìm kiếm phòng khám theo từ khóa (số phòng, tòa nhà), lọc theo tòa nhà (building) và trạng thái hoạt động (isActive) kèm phân trang."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách phòng khám thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực danh tính"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập")
    })
    public ApiResponse<PageResponse<ExaminationRoomResponse>> getRooms(
            @Parameter(description = "Từ khóa tìm kiếm (số phòng hoặc tòa nhà)", example = "201")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Lọc theo khu nhà / tòa nhà cụ thể", example = "Khu A - Tầng 2")
            @RequestParam(required = false) String building,

            @Parameter(description = "Lọc theo trạng thái hoạt động (true: Đang hoạt động, false: Tạm dừng / Bảo trì)", example = "true")
            @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "Số thứ tự trang (bắt đầu từ 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Số lượng bản ghi trên một trang", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Trường dùng để sắp xếp", example = "roomNumber")
            @RequestParam(defaultValue = "roomNumber") String sortBy,

            @Parameter(description = "Hướng sắp xếp (asc hoặc desc)", example = "asc")
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        PageResponse<ExaminationRoomResponse> response = examinationRoomService.getRooms(keyword, building, isActive, page, size, sortBy, sortDir);
        return ApiResponse.success("Lấy danh sách phòng khám thành công", response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({RoleConstant.ADMIN})
    @Operation(
            summary = "Thêm mới phòng khám (Admin)",
            description = "Dành riêng cho Quản trị viên. Tạo phòng khám mới trong hệ thống. Kiểm tra tránh trùng số phòng trong cùng khu nhà."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo mới phòng khám thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Số phòng đã tồn tại trong cùng khu nhà"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Vi phạm validation")
    })
    public ApiResponse<ExaminationRoomResponse> createRoom(@Valid @RequestBody ExaminationRoomCreateRequest request) {
        ExaminationRoomResponse response = examinationRoomService.createRoom(request);
        return ApiResponse.success("Thêm mới phòng khám thành công", response);
    }

    @PutMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN})
    @Operation(
            summary = "Cập nhật thông tin phòng khám (Admin)",
            description = "Dành riêng cho Quản trị viên. Cập nhật số phòng, tòa nhà hoặc trạng thái hoạt động của phòng."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật phòng khám thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng khám"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Số phòng trùng với phòng khác trong cùng khu nhà")
    })
    public ApiResponse<ExaminationRoomResponse> updateRoom(
            @Parameter(description = "ID phòng khám cần cập nhật", example = "1", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody ExaminationRoomUpdateRequest request
    ) {
        ExaminationRoomResponse response = examinationRoomService.updateRoom(id, request);
        return ApiResponse.success("Cập nhật thông tin phòng khám thành công", response);
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @RequireRole({RoleConstant.ADMIN})
    @Operation(
            summary = "Bật / Tắt trạng thái phòng khám (Admin) [Hỗ trợ PATCH/PUT]",
            description = "Dành riêng cho Quản trị viên. Chuyển đổi trạng thái giữa Sẵn sàng hoạt động (true) và Đang bảo trì/Sửa chữa (false). Khi tắt, phòng khám sẽ không xuất hiện trong dropdown xếp lịch."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng khám")
    })
    public ApiResponse<ExaminationRoomResponse> updateRoomStatus(
            @Parameter(description = "ID phòng khám cần cập nhật trạng thái", example = "1", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody UpdateRoomStatusRequest request
    ) {
        ExaminationRoomResponse response = examinationRoomService.updateRoomStatus(id, request);
        String action = Boolean.TRUE.equals(request.getIsActive()) ? "Kích hoạt" : "Chuyển sang bảo trì";
        return ApiResponse.success(action + " phòng khám thành công", response);
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN})
    @Operation(
            summary = "Xóa phòng khám (Admin)",
            description = "Dành riêng cho Quản trị viên. Chỉ xóa được khi chưa có ca trực lịch làm việc của bác sĩ hoặc lịch sử vật tư gắn với phòng này. Nếu đã có dữ liệu, hãy chuyển trạng thái sang Tạm ngưng/Bảo trì."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa phòng khám thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy phòng khám"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xóa do vướng lịch trực bác sĩ hoặc dữ liệu liên kết")
    })
    public ApiResponse<Void> deleteRoom(
            @Parameter(description = "ID phòng khám cần xóa", example = "1", required = true)
            @PathVariable Integer id
    ) {
        examinationRoomService.deleteRoom(id);
        return ApiResponse.success("Xóa phòng khám thành công", null);
    }
}

