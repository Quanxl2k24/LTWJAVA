package org.example.feature.timeslot;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.api.ApiResponse;
import org.example.common.security.RequireRole;
import org.example.common.security.RoleConstant;
import org.example.feature.timeslot.dto.TimeSlotCreateRequest;
import org.example.feature.timeslot.dto.TimeSlotResponse;
import org.example.feature.timeslot.dto.TimeSlotUpdateRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/time-slots")
@RequiredArgsConstructor
@Tag(name = "Time Slot Feature", description = "Các API quản lý khung giờ khám bệnh cố định trong ngày, phục vụ việc đặt lịch khám của bệnh nhân và xếp ca trực của bác sĩ")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    // =========================================================================
    // 1. NHÓM API CÔNG KHAI (PUBLIC CHO BỆNH NHÂN & ĐẶT LỊCH)
    // =========================================================================

    @GetMapping
    @Operation(
            summary = "Lấy danh sách các khung giờ khám bệnh trong ngày",
            description = "API công khai (không cần đăng nhập). Trả về toàn bộ danh mục khung giờ khám bệnh được sắp xếp từ sớm đến muộn. Có thể lọc theo buổi (morning, afternoon, evening). Phục vụ bệnh nhân chọn giờ khi đặt lịch khám trực tuyến."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách khung giờ thành công")
    })
    public ApiResponse<List<TimeSlotResponse>> getAllTimeSlots(
            @Parameter(description = "Lọc theo buổi trong ngày (morning: Sáng, afternoon: Chiều, evening: Tối)", example = "morning")
            @RequestParam(required = false) String session
    ) {
        List<TimeSlotResponse> slots = timeSlotService.getAllTimeSlots(session);
        return ApiResponse.success("Lấy danh sách khung giờ khám thành công", slots);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Xem thông tin chi tiết một khung giờ khám theo ID",
            description = "API công khai. Trả về thời gian bắt đầu, thời gian kết thúc và buổi trong ngày của khung giờ."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy chi tiết khung giờ thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy khung giờ khám")
    })
    public ApiResponse<TimeSlotResponse> getTimeSlotById(
            @Parameter(description = "ID của khung giờ khám", example = "1", required = true)
            @PathVariable Integer id
    ) {
        TimeSlotResponse response = timeSlotService.getTimeSlotById(id);
        return ApiResponse.success("Lấy thông tin khung giờ thành công", response);
    }

    // =========================================================================
    // 2. NHÓM API QUẢN TRỊ VIÊN (ADMIN)
    // =========================================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Tạo mới khung giờ khám (Admin)",
            description = "Dành riêng cho Quản trị viên. Tạo khung giờ khám mới. Hệ thống sẽ tự động kiểm tra giờ bắt đầu phải trước giờ kết thúc và không được chồng chéo (overlap) với các khung giờ đã có."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo mới khung giờ khám thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Thời gian không hợp lệ (giờ kết thúc trước giờ bắt đầu)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Khung giờ bị trùng lặp hoặc chồng chéo thời gian"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dữ liệu vi phạm validation")
    })
    public ApiResponse<TimeSlotResponse> createTimeSlot(@Valid @RequestBody TimeSlotCreateRequest request) {
        TimeSlotResponse response = timeSlotService.createTimeSlot(request);
        return ApiResponse.success("Tạo mới khung giờ khám thành công", response);
    }

    @PutMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Cập nhật khung giờ khám (Admin)",
            description = "Dành riêng cho Quản trị viên. Chỉnh sửa thời gian bắt đầu, kết thúc hoặc buổi của khung giờ khám."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật khung giờ thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Thời gian không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy khung giờ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Thời gian cập nhật bị chồng chéo với khung giờ khác")
    })
    public ApiResponse<TimeSlotResponse> updateTimeSlot(
            @Parameter(description = "ID khung giờ khám cần cập nhật", example = "1", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody TimeSlotUpdateRequest request
    ) {
        TimeSlotResponse response = timeSlotService.updateTimeSlot(id, request);
        return ApiResponse.success("Cập nhật khung giờ khám thành công", response);
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Xóa khung giờ khám (Admin)",
            description = "Dành riêng cho Quản trị viên. Chỉ xóa được khi chưa có ca trực lịch khám nào của bác sĩ sử dụng khung giờ này."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa khung giờ thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy khung giờ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xóa do đã có ca trực của bác sĩ sử dụng khung giờ này")
    })
    public ApiResponse<Void> deleteTimeSlot(
            @Parameter(description = "ID khung giờ khám cần xóa", example = "1", required = true)
            @PathVariable Integer id
    ) {
        timeSlotService.deleteTimeSlot(id);
        return ApiResponse.success("Xóa khung giờ khám thành công", null);
    }
}

