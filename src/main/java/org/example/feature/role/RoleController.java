package org.example.feature.role;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.common.api.ApiResponse;
import org.example.common.security.CurrentUser;
import org.example.common.security.RequireRole;
import org.example.common.security.RoleConstant;
import org.example.common.security.UserPrincipal;
import org.example.feature.role.dto.RoleResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/roles", "/api/v1/role"})
@RequiredArgsConstructor
@Tag(name = "Role Feature", description = "Các API quản lý danh mục vai trò người dùng")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả các vai trò", description = "Trả về toàn bộ danh mục vai trò hiện có trong hệ thống kèm mã code và tên hiển thị")
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> roles = roleService.getAllRoles();
        return ApiResponse.success("Lấy danh sách vai trò thành công", roles);
    }

    @GetMapping("/admin-only")
    @RequireRole({RoleConstant.ADMIN})
    @Operation(summary = "API chỉ dành cho Admin", description = "Chỉ tài khoản có vai trò ADMIN mới được phép truy cập")
    public ApiResponse<String> adminOnlyEndpoint(@CurrentUser UserPrincipal currentUser) {
        return ApiResponse.success("Chào mừng Admin: " + currentUser.getFullName(), currentUser.getEmail());
    }

    @GetMapping("/staff-or-admin")
    @RequireRole({RoleConstant.ADMIN, RoleConstant.STAFF})
    @Operation(summary = "API cho phép nhiều vai trò", description = "Cả tài khoản ADMIN và STAFF đều có thể truy cập")
    public ApiResponse<String> multiRoleEndpoint(@CurrentUser UserPrincipal currentUser) {
        return ApiResponse.success("Truy cập hợp lệ với vai trò: " + currentUser.getRole(), currentUser.getRole());
    }
}
