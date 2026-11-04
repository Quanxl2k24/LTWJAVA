package org.example.feature.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.feature.auth.dto.*;
import org.example.feature.auth.model.Role;
import org.example.feature.auth.model.User;
import org.example.feature.auth.model.UserSession;
import org.example.feature.auth.repository.RoleRepository;
import org.example.feature.auth.repository.UserRepository;
import org.example.feature.auth.repository.UserSessionRepository;
import org.example.feature.auth.util.CookieUtil;
import org.example.feature.auth.util.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        // 1. Kiểm tra số điện thoại đã tồn tại chưa
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        // 2. Kiểm tra email nếu người dùng có cung cấp
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail().trim())) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }

        // 3. Gán quyền mặc định là Bệnh nhân (PATIENT)
        Role patientRole = roleRepository.findByCode("PATIENT")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .code("PATIENT")
                        .name("Bệnh nhân")
                        .description("Bệnh nhân")
                        .build()));

        // 4. Mã hóa mật khẩu
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // 5. Lưu thông tin người dùng
        User user = User.builder()
                .role(patientRole)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null ? request.getEmail().trim() : null)
                .passwordHash(hashedPassword)
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user successfully: id={}, phone={}, role={}", 
                savedUser.getId(), savedUser.getPhone(), patientRole.getCode());

        return RegisterResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .phone(savedUser.getPhone())
                .email(savedUser.getEmail())
                .role(savedUser.getRole() != null ? savedUser.getRole().getCode() : "PATIENT")
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String identifier = request.getIdentifier().trim();

        // 1. Tìm người dùng bằng Email hoặc Số điện thoại
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhone(identifier))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        // 2. Kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: Incorrect password for identifier: {}", identifier);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 3. Kiểm tra trạng thái tài khoản
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        // 4. Tạo accessToken (sống trong 15 phút)
        String accessToken = jwtTokenProvider.generateAccessToken(user);

        // 5. Tạo raw refreshToken (sống trong 30 ngày)
        String rawRefreshToken = jwtTokenProvider.generateRawRefreshToken();
        String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);
        Instant expiresAt = Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs());

        // 6. Lưu phiên vào bảng user_sessions
        UserSession session = UserSession.builder()
                .user(user)
                .tokenHash(tokenHash)
                .deviceId(request.getDeviceId())
                .deviceName(request.getDeviceName())
                .ipAddress(getClientIp(httpRequest))
                .userAgent(httpRequest.getHeader("User-Agent"))
                .isRevoked(false)
                .expiresAt(expiresAt)
                .build();

        userSessionRepository.save(session);

        // 7. Lưu refreshToken vào HttpOnly Cookie
        cookieUtil.addRefreshTokenCookie(httpResponse, rawRefreshToken);

        log.info("User logged in successfully: userId={}, role={}", user.getId(), user.getRole().getCode());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .user(LoginResponse.UserSummary.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .role(user.getRole() != null ? user.getRole().getCode() : "PATIENT")
                        .build())
                .build();
    }

    @Transactional
    public TokenResponse refreshToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        // 1. Đọc refreshToken từ Cookie
        String rawRefreshToken = cookieUtil.getRefreshTokenFromCookie(httpRequest)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN, "Không tìm thấy refresh token trong cookie"));

        // 2. Hash và tra cứu trong database
        String tokenHash = jwtTokenProvider.hashToken(rawRefreshToken);
        UserSession session = userSessionRepository.findByTokenHashAndIsRevokedFalse(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN, "Phiên đăng nhập không hợp lệ hoặc đã bị hủy"));

        // 3. Kiểm tra hạn dùng
        if (session.getExpiresAt().isBefore(Instant.now())) {
            session.setIsRevoked(true);
            userSessionRepository.save(session);
            cookieUtil.deleteRefreshTokenCookie(httpResponse);
            throw new AppException(ErrorCode.INVALID_TOKEN, "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
        }

        User user = session.getUser();
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.USER_INACTIVE);
        }

        // 4. Cập nhật thời gian sử dụng gần nhất
        session.setLastUsedAt(Instant.now());
        userSessionRepository.save(session);

        // 5. Cấp mới accessToken (15 phút)
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .build();
    }

    @Transactional
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        cookieUtil.getRefreshTokenFromCookie(httpRequest).ifPresent(rawToken -> {
            String tokenHash = jwtTokenProvider.hashToken(rawToken);
            userSessionRepository.findByTokenHashAndIsRevokedFalse(tokenHash).ifPresent(session -> {
                session.setIsRevoked(true);
                userSessionRepository.save(session);
                log.info("Revoked user session on logout: sessionId={}", session.getId());
            });
        });

        cookieUtil.deleteRefreshTokenCookie(httpResponse);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
