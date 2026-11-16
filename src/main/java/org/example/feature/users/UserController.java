package org.example.feature.users;

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
import org.example.feature.users.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Feature", description = "Các API phục vụ quản lý người dùng, phân quyền truy cập và quản lý hồ sơ cá nhân")
@SecurityRequirement(name = "BearerAuth")
public class UserController {

    private final UserService userService;

    // =========================================================================
    // 1. NHÓM QUẢN TRỊ VIÊN & NHÂN VIÊN (ADMIN & STAFF)
    // =========================================================================

    @GetMapping
    @RequireRole({RoleConstant.ADMIN, RoleConstant.STAFF})
    @Operation(
            summary = "Lấy danh sách người dùng (Phân trang, Lọc & Tìm kiếm)",
            description = "Dành cho Admin và Lễ tân. Hỗ trợ tìm kiếm theo từ khóa (họ tên, sđt, email), lọc theo vai trò (roleId), trạng thái hoạt động (isActive) và phân trang."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách người dùng thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Không có quyền truy cập (Yêu cầu vai trò ADMIN hoặc STAFF)")
    })
    public ApiResponse<PageResponse<UserResponse>> getUsers(
            @Parameter(description = "Từ khóa tìm kiếm (họ tên, số điện thoại hoặc email)", example = "Nguyễn")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Lọc theo ID vai trò (1: ADMIN, 2: DOCTOR, 3: STAFF, 4: PATIENT)", example = "2")
            @RequestParam(required = false) Integer roleId,

            @Parameter(description = "Lọc theo trạng thái hoạt động (true: Đang hoạt động, false: Đã khóa)", example = "true")
            @RequestParam(required = false) Boolean isActive,

            @Parameter(description = "Số thứ tự trang (bắt đầu từ 1)", example = "1")
            @RequestParam(defaultValue = "1") int page,

            @Parameter(description = "Số lượng bản ghi trên một trang", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Trường dùng để sắp xếp (ví dụ: id, fullName, createdAt)", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,

            @Parameter(description = "Hướng sắp xếp (asc hoặc desc)", example = "desc")
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        PageResponse<UserResponse> response = userService.getUsers(keyword, roleId, isActive, page, size, sortBy, sortDir);
        return ApiResponse.success("Lấy danh sách người dùng thành công", response);
    }

    @GetMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.STAFF})
    @Operation(
            summary = "Xem thông tin chi tiết người dùng theo ID",
            description = "Dành cho Admin và Lễ tân. Trả về thông tin đầy đủ của một người dùng bao gồm thông tin cá nhân và vai trò."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy chi tiết thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng với ID đã cung cấp")
    })
    public ApiResponse<UserResponse> getUserById(
            @Parameter(description = "ID người dùng cần xem", example = "1", required = true)
            @PathVariable Long id
    ) {
        UserResponse response = userService.getUserById(id);
        return ApiResponse.success("Lấy thông tin người dùng thành công", response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequireRole({RoleConstant.ADMIN})
    @Operation(
            summary = "Tạo mới tài khoản người dùng (Admin)",
            description = "Dành riêng cho Quản trị viên (ADMIN) để tạo tài khoản Bác sĩ, Lễ tân hoặc Admin khác. Nếu bỏ trống mật khẩu, hệ thống tự cấp mật khẩu mặc định 'Abc@123456'."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo tài khoản người dùng thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Số điện thoại hoặc Email đã được đăng ký trước đó"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Dữ liệu vi phạm điều kiện validation")
    })
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ApiResponse.success("Tạo tài khoản người dùng thành công", response);
    }

    @PutMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN})
    @Operation(
            summary = "Cập nhật thông tin người dùng (Admin)",
            description = "Dành riêng cho Quản trị viên (ADMIN) để cập nhật thông tin họ tên, email, vai trò, ngày sinh, giới tính, địa chỉ của người dùng."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thông tin người dùng thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng hoặc vai trò"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email cập nhật bị trùng với tài khoản khác")
    })
    public ApiResponse<UserResponse> updateUser(
            @Parameter(description = "ID người dùng cần cập nhật", example = "2", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        UserResponse response = userService.updateUser(id, request);
        return ApiResponse.success("Cập nhật thông tin người dùng thành công", response);
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @RequireRole({RoleConstant.ADMIN})
    @Operation(
            summary = "Khóa hoặc Mở khóa tài khoản (Admin) [Hỗ trợ PATCH/PUT]",
            description = "Dành riêng cho Quản trị viên. Khi khóa tài khoản (`isActive = false`), toàn bộ các phiên đăng nhập (Refresh Tokens) hiện tại của user sẽ tự động bị thu hồi ngay lập tức."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật trạng thái tài khoản thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    public ApiResponse<UserResponse> updateUserStatus(
            @Parameter(description = "ID người dùng cần thay đổi trạng thái", example = "3", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        UserResponse response = userService.updateUserStatus(id, request);
        String action = Boolean.TRUE.equals(request.getIsActive()) ? "Mở khóa" : "Khóa";
        return ApiResponse.success(action + " tài khoản thành công", response);
    }

    @RequestMapping(value = "/{id}/reset-password", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @RequireRole({RoleConstant.ADMIN})
    @Operation(
            summary = "Đặt lại mật khẩu người dùng (Admin) [Hỗ trợ PATCH/PUT]",
            description = "Dành riêng cho Quản trị viên khi người dùng bị mất/quên mật khẩu. Sau khi reset, toàn bộ phiên đăng nhập cũ của user sẽ bị hủy bỏ."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đặt lại mật khẩu thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    public ApiResponse<Void> resetPassword(
            @Parameter(description = "ID người dùng cần reset mật khẩu", example = "4", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        userService.resetPassword(id, request);
        return ApiResponse.success("Đặt lại mật khẩu cho người dùng thành công", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole({RoleConstant.ADMIN})
    @Operation(
            summary = "Xóa tài khoản người dùng (Admin)",
            description = "Chỉ xóa được tài khoản nếu chưa phát sinh bất kỳ hồ sơ khám, lịch hẹn, giao dịch kho hoặc nhật ký trong hệ thống. Nếu đã có dữ liệu, hãy dùng tính năng Khóa tài khoản."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa người dùng thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Không thể xóa do vướng dữ liệu liên kết (FK constraint)")
    })
    public ApiResponse<Void> deleteUser(
            @Parameter(description = "ID người dùng cần xóa", example = "5", required = true)
            @PathVariable Long id
    ) {
        userService.deleteUser(id);
        return ApiResponse.success("Xóa tài khoản người dùng thành công", null);
    }

    // =========================================================================
    // 2. NHÓM NGƯỜI DÙNG CÁ NHÂN (SELF PROFILE & ACCOUNT)
    // =========================================================================

    @GetMapping("/profile")
    @RequireRole
    @Operation(
            summary = "Xem hồ sơ cá nhân của người dùng hiện tại",
            description = "Mọi tài khoản đã đăng nhập đều có thể gọi API này để xem thông tin chi tiết của chính mình dựa trên Access Token."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy hồ sơ cá nhân thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    public ApiResponse<UserResponse> getMyProfile(
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser
    ) {
        UserResponse response = userService.getMyProfile(currentUser.getId());
        return ApiResponse.success("Lấy hồ sơ cá nhân thành công", response);
    }

    @PutMapping("/profile")
    @RequireRole
    @Operation(
            summary = "Cập nhật hồ sơ cá nhân",
            description = "Cho phép người dùng tự sửa họ tên, ngày sinh, giới tính và địa chỉ của chính mình (không được tự ý sửa số điện thoại hay vai trò)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật hồ sơ thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    public ApiResponse<UserResponse> updateMyProfile(
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        UserResponse response = userService.updateMyProfile(currentUser.getId(), request);
        return ApiResponse.success("Cập nhật hồ sơ cá nhân thành công", response);
    }

    @RequestMapping(value = "/change-password", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @RequireRole
    @Operation(
            summary = "Đổi mật khẩu tài khoản cá nhân [Hỗ trợ PATCH/PUT]",
            description = "Người dùng tự đổi mật khẩu bằng cách xác nhận mật khẩu hiện tại và nhập mật khẩu mới. Đổi thành công sẽ tự động hủy các phiên đăng nhập khác."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đổi mật khẩu thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Mật khẩu hiện tại sai hoặc mật khẩu mới xác nhận không khớp"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa xác thực")
    })
    public ApiResponse<Void> changeMyPassword(
            @Parameter(hidden = true) @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changeMyPassword(currentUser.getId(), request);
        return ApiResponse.success("Đổi mật khẩu thành công", null);
    }
}

