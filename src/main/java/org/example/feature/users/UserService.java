package org.example.feature.users;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.api.PageResponse;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.feature.auth.model.Role;
import org.example.feature.auth.model.User;
import org.example.feature.auth.repository.RoleRepository;
import org.example.feature.auth.repository.UserRepository;
import org.example.feature.auth.repository.UserSessionRepository;
import org.example.feature.role.dto.RoleResponse;
import org.example.feature.users.dto.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_NEW_USER_PASSWORD = "Abc@123456";

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(
            String keyword,
            Integer roleId,
            Boolean isActive,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, sort);

        Page<User> userPage = userRepository.searchUsers(
                keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                roleId,
                isActive,
                pageable
        );

        List<UserResponse> items = userPage.getContent().stream()
                .map(this::mapToUserResponse)
                .toList();

        return PageResponse.<UserResponse>builder()
                .items(items)
                .pageNumber(userPage.getNumber() + 1)
                .pageSize(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .isLast(userPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findUserByIdOrThrow(id);
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS, "Số điện thoại đã được đăng ký trên hệ thống");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email đã được đăng ký trên hệ thống");
            }
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND, "Vai trò chỉ định không tồn tại"));

        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : DEFAULT_NEW_USER_PASSWORD;

        User user = User.builder()
                .phone(request.getPhone().trim())
                .email(request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim().toLowerCase() : null)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(request.getFullName().trim())
                .role(role)
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Quản trị viên đã tạo tài khoản mới: ID={}, Phone={}, Role={}", savedUser.getId(), savedUser.getPhone(), role.getCode());
        return mapToUserResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findUserByIdOrThrow(id);

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String normalizedEmail = request.getEmail().trim().toLowerCase();
            if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email đã được sử dụng bởi một tài khoản khác");
            }
            user.setEmail(normalizedEmail);
        } else {
            user.setEmail(null);
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND, "Vai trò chỉ định không tồn tại"));

        user.setFullName(request.getFullName().trim());
        user.setRole(role);
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress());
        if (request.getIsActive() != null) {
            user.setIsActive(request.getIsActive());
            if (!request.getIsActive()) {
                userSessionRepository.revokeAllSessionsByUserId(id);
            }
        }
        user.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(user);
        log.info("Quản trị viên đã cập nhật tài khoản ID={}", id);
        return mapToUserResponse(updatedUser);
    }

    @Transactional
    public UserResponse updateUserStatus(Long id, UpdateUserStatusRequest request) {
        User user = findUserByIdOrThrow(id);
        user.setIsActive(request.getIsActive());
        user.setUpdatedAt(Instant.now());

        if (Boolean.FALSE.equals(request.getIsActive())) {
            // Khi tài khoản bị vô hiệu hóa, thu hồi toàn bộ phiên đăng nhập hiện tại
            userSessionRepository.revokeAllSessionsByUserId(id);
            log.info("Tài khoản ID={} bị tạm khóa. Đã thu hồi toàn bộ phiên đăng nhập.", id);
        } else {
            log.info("Tài khoản ID={} đã được kích hoạt lại.", id);
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = findUserByIdOrThrow(id);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Hủy các phiên đăng nhập cũ để bắt buộc đăng nhập bằng mật khẩu mới
        userSessionRepository.revokeAllSessionsByUserId(id);
        log.info("Quản trị viên đã đặt lại mật khẩu cho tài khoản ID={}", id);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findUserByIdOrThrow(id);
        try {
            userRepository.delete(user);
            userRepository.flush();
            log.info("Đã xóa vĩnh viễn tài khoản ID={}", id);
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể xóa tài khoản ID={} do ràng buộc khóa ngoại: {}", id, e.getMessage());
            throw new AppException(
                    ErrorCode.CANNOT_DELETE_USER_WITH_DEPENDENCIES,
                    "Không thể xóa người dùng do tài khoản này đã có hồ sơ khám bệnh, lịch hẹn hoặc dữ liệu liên quan. Vui lòng sử dụng tính năng khóa tài khoản."
            );
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long currentUserId) {
        User user = findUserByIdOrThrow(currentUserId);
        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateMyProfile(Long currentUserId, UserProfileUpdateRequest request) {
        User user = findUserByIdOrThrow(currentUserId);

        user.setFullName(request.getFullName().trim());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAddress(request.getAddress());
        user.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(user);
        log.info("Người dùng ID={} đã cập nhật thông tin cá nhân", currentUserId);
        return mapToUserResponse(updatedUser);
    }

    @Transactional
    public void changeMyPassword(Long currentUserId, ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH, "Mật khẩu mới và xác nhận mật khẩu không trùng khớp");
        }

        User user = findUserByIdOrThrow(currentUserId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT, "Mật khẩu hiện tại không chính xác");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Hủy tất cả các phiên đăng nhập khác của user
        userSessionRepository.revokeAllSessionsByUserId(currentUserId);
        log.info("Người dùng ID={} đã đổi mật khẩu thành công", currentUserId);
    }

    private User findUserByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với ID: " + id));
    }

    private UserResponse mapToUserResponse(User user) {
        Role role = user.getRole();
        RoleResponse roleResponse = role != null ? RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .build() : null;

        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .isActive(user.getIsActive())
                .role(roleResponse)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

