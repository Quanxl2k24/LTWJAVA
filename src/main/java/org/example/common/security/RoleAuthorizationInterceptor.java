package org.example.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.feature.auth.util.JwtTokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * Interceptor kiểm tra xác thực (Authentication) và phân quyền (Authorization)
 * dựa trên chú thích {@link RequireRole}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleAuthorizationInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Bỏ qua nếu không phải request gọi vào Controller method (ví dụ OPTIONS, static resources)
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Tìm annotation @RequireRole ở cấp Method trước, nếu không có thì tìm ở cấp Class
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Nếu endpoint không có @RequireRole (public endpoint)
        // Vẫn cố gắng trích xuất UserPrincipal nếu client có gửi Bearer Token hợp lệ
        if (requireRole == null) {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (!token.isEmpty() && jwtTokenProvider.validateToken(token)) {
                    UserPrincipal principal = buildPrincipalFromToken(token);
                    request.setAttribute(CurrentUserArgumentResolver.CURRENT_USER_ATTRIBUTE, principal);
                }
            }
            return true;
        }

        // 1. Kiểm tra header Authorization bắt buộc cho endpoint có @RequireRole
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập và cung cấp Bearer Access Token");
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty() || !jwtTokenProvider.validateToken(token)) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Access Token không hợp lệ hoặc đã hết hạn");
        }

        // 2. Trích xuất thông tin người dùng từ token claims
        UserPrincipal principal = buildPrincipalFromToken(token);

        // Lưu thông tin người dùng vào request attribute để dùng cho @CurrentUser
        request.setAttribute(CurrentUserArgumentResolver.CURRENT_USER_ATTRIBUTE, principal);

        // 3. Kiểm tra phân quyền vai trò (Role Authorization)
        String userRole = principal.getRole();
        String[] allowedRoles = requireRole.value();
        if (allowedRoles.length > 0) {
            if (userRole == null || userRole.isBlank()) {
                throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản chưa được gán vai trò truy cập");
            }

            boolean isAuthorized = Arrays.stream(allowedRoles)
                    .anyMatch(allowed -> allowed.equalsIgnoreCase(userRole.trim()));

            if (!isAuthorized) {
                log.warn("Truy cập bị từ chối: User id={} với vai trò '{}' yêu cầu một trong các vai trò {}",
                        principal.getId(), userRole, Arrays.toString(allowedRoles));
                throw new AppException(ErrorCode.FORBIDDEN,
                        "Bạn không có quyền thực hiện thao tác này. Yêu cầu một trong các vai trò: " + Arrays.toString(allowedRoles));
            }
        }

        return true;
    }

    private UserPrincipal buildPrincipalFromToken(String token) {
        Claims claims = jwtTokenProvider.extractClaims(token);
        Long userId = null;
        try {
            userId = Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            log.warn("Subject trong JWT không phải là userId hợp lệ: {}", claims.getSubject());
        }

        return UserPrincipal.builder()
                .id(userId)
                .email(claims.get("email", String.class))
                .phone(claims.get("phone", String.class))
                .role(claims.get("role", String.class))
                .fullName(claims.get("fullName", String.class))
                .build();
    }
}

