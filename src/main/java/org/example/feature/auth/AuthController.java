package org.example.feature.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.api.ApiResponse;
import org.example.feature.auth.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Feature", description = "Các API phục vụ xác thực người dùng (Đăng ký, Đăng nhập, Refresh Token, Đăng xuất)")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đăng ký tài khoản mới", description = "Đăng ký người dùng mới với vai trò mặc định là Bệnh nhân (PATIENT)")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ApiResponse.success("Đăng ký tài khoản thành công", response);
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống", description = "Đăng nhập bằng Email hoặc Số điện thoại. Trả về Access Token (15 phút) trong JSON và Refresh Token (30 ngày) lưu trong HttpOnly Cookie.")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        LoginResponse response = authService.login(request, httpRequest, httpResponse);
        return ApiResponse.success("Đăng nhập thành công", response);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới Access Token", description = "Đọc Refresh Token từ HttpOnly Cookie để cấp Access Token mới (15 phút)")
    public ApiResponse<TokenResponse> refreshToken(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        TokenResponse response = authService.refreshToken(httpRequest, httpResponse);
        return ApiResponse.success("Làm mới token thành công", response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất tài khoản", description = "Hủy phiên đăng nhập trong cơ sở dữ liệu và xóa HttpOnly Cookie")
    public ApiResponse<Void> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        authService.logout(httpRequest, httpResponse);
        return ApiResponse.success("Đăng xuất thành công", null);
    }
}

