package org.example;

import org.example.common.api.ApiResponse;
import org.example.common.api.PageResponse;
import org.example.feature.auth.dto.*;
import org.example.feature.doctorprofile.dto.*;
import org.example.feature.hello.dto.HelloResponse;
import org.example.feature.medicalservice.dto.*;
import org.example.feature.room.dto.*;
import org.example.feature.specialty.dto.*;
import org.example.feature.timeslot.dto.*;
import org.example.feature.users.dto.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApplicationTest {

    @Value(value = "${local.server.port}")
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setupRestTemplate() {
        if (restTemplate.getRestTemplate().getRequestFactory() instanceof org.springframework.http.client.SimpleClientHttpRequestFactory factory) {
            factory.setOutputStreaming(false);
        }
    }

    private static String savedRefreshTokenCookie;
    private static String savedPatientAccessToken;
    private static String savedAdminAccessToken;
    private static Long createdDoctorUserId;
    private static Integer createdSpecialtyId;
    private static Integer createdRoomId;
    private static Integer createdTimeSlotId;
    private static Integer doctorTestSpecialtyId;
    private static Long createdDoctorProfileId;
    private static String savedDoctorAccessToken;
    private static Integer createdSpecialtyServiceId;
    private static Integer createdGeneralServiceId;

    @Test
    @Order(1)
    void contextLoads() {
    }

    @Test
    @Order(2)
    void shouldRespondWithStandardApiResponseForHello() {
        ResponseEntity<ApiResponse<HelloResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/hello",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<HelloResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData().getMessage()).isEqualTo("Hello World!");
    }

    @Test
    @Order(5)
    void shouldRegisterNewPatientSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Nguyễn Văn An")
                .phone("0912345678")
                .email("nguyenvanan@test.com")
                .password("Password@123")
                .gender("male")
                .address("Hà Nội")
                .build();

        ResponseEntity<ApiResponse<RegisterResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<RegisterResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData().getPhone()).isEqualTo("0912345678");
        assertThat(response.getBody().getData().getEmail()).isEqualTo("nguyenvanan@test.com");
        assertThat(response.getBody().getData().getRole()).isEqualTo("PATIENT");
    }

    @Test
    @Order(6)
    void shouldFailRegistrationWhenPhoneAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Trùng Số Điện Thoại")
                .phone("0912345678")
                .email("other@test.com")
                .password("Password@123")
                .build();

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(2004);
    }

    @Test
    @Order(7)
    void shouldLoginSuccessfullyAndReturnAccessTokenAndRefreshTokenCookie() {
        LoginRequest request = LoginRequest.builder()
                .identifier("0912345678")
                .password("Password@123")
                .build();

        ResponseEntity<ApiResponse<LoginResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        LoginResponse loginData = response.getBody().getData();
        assertThat(loginData.getAccessToken()).isNotBlank();
        assertThat(loginData.getExpiresIn()).isEqualTo(900); // 15 mins
        assertThat(loginData.getUser().getPhone()).isEqualTo("0912345678");

        // Kiểm tra HttpOnly Cookie cho Refresh Token (30 ngày)
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotEmpty();
        String refreshCookie = cookies.stream()
                .filter(c -> c.startsWith("refreshToken="))
                .findFirst()
                .orElse(null);

        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie).contains("HttpOnly");
        assertThat(refreshCookie).contains("Max-Age=2592000"); // 30 ngày = 2,592,000s

        savedRefreshTokenCookie = refreshCookie.split(";")[0]; // "refreshToken=..."
        savedPatientAccessToken = loginData.getAccessToken();
    }

    @Test
    @Order(8)
    void shouldFailLoginWithIncorrectPassword() {
        LoginRequest request = LoginRequest.builder()
                .identifier("0912345678")
                .password("WrongPassword")
                .build();

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(2002);
    }

    @Test
    @Order(9)
    void shouldRefreshTokenSuccessfullyUsingCookie() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, savedRefreshTokenCookie);

        ResponseEntity<ApiResponse<TokenResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/refresh-token",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<ApiResponse<TokenResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getData().getExpiresIn()).isEqualTo(900);
    }

    @Test
    @Order(10)
    void shouldLogoutAndClearCookie() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, savedRefreshTokenCookie);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/logout",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(cookies).isNotEmpty();
        String clearedCookie = cookies.stream()
                .filter(c -> c.startsWith("refreshToken="))
                .findFirst()
                .orElse(null);

        assertThat(clearedCookie).isNotNull();
        assertThat(clearedCookie).contains("Max-Age=0");
    }

    @Test
    @Order(11)
    void shouldGetAllRolesSuccessfully() {
        ResponseEntity<ApiResponse<List<org.example.feature.role.dto.RoleResponse>>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/roles",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<org.example.feature.role.dto.RoleResponse>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);

        List<org.example.feature.role.dto.RoleResponse> roles = response.getBody().getData();
        assertThat(roles).isNotEmpty();
        assertThat(roles).hasSize(4);
        assertThat(roles).extracting(org.example.feature.role.dto.RoleResponse::getCode)
                .containsExactlyInAnyOrder("ADMIN", "DOCTOR", "STAFF", "PATIENT");
    }

    @Test
    @Order(12)
    void shouldRejectAccessWhenNoTokenProvided() {
        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/me",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(401);
    }

    @Test
    @Order(13)
    void shouldRejectAccessWhenTokenIsInvalid() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("invalid.fake.jwt.token");

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(2006); // INVALID_TOKEN
    }

    @Test
    @Order(14)
    void shouldAllowAccessToMeEndpointWithValidToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedPatientAccessToken);

        ResponseEntity<ApiResponse<org.example.common.security.UserPrincipal>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<org.example.common.security.UserPrincipal>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        assertThat(response.getBody().getData().getRole()).isEqualTo("PATIENT");
        assertThat(response.getBody().getData().getPhone()).isEqualTo("0912345678");
    }

    @Test
    @Order(15)
    void shouldReturnForbiddenWhenPatientAccessesAdminOnlyEndpoint() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedPatientAccessToken);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/roles/admin-only",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(403);
    }

    @Test
    @Order(16)
    void shouldReturnForbiddenWhenPatientAccessesMultiRoleEndpointRequiringAdminOrStaff() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedPatientAccessToken);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/roles/staff-or-admin",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(403);
    }

    @Test
    @Order(17)
    void shouldAllowAdminToAccessBothAdminAndMultiRoleEndpoints() {
        // 1. Đăng nhập với tài khoản Admin mặc định
        LoginRequest adminLogin = LoginRequest.builder()
                .identifier("admin@hospital.com")
                .password("Admin@123456")
                .build();

        ResponseEntity<ApiResponse<LoginResponse>> loginResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(adminLogin),
                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String adminAccessToken = loginResponse.getBody().getData().getAccessToken();
        assertThat(adminAccessToken).isNotBlank();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminAccessToken);

        // 2. Thử gọi API admin-only
        ResponseEntity<ApiResponse<String>> adminOnlyResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/roles/admin-only",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<ApiResponse<String>>() {}
        );
        assertThat(adminOnlyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminOnlyResponse.getBody().getCode()).isEqualTo(200);

        // 3. Thử gọi API cho phép nhiều vai trò (ADMIN hoặc STAFF)
        ResponseEntity<ApiResponse<String>> multiRoleResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/roles/staff-or-admin",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<ApiResponse<String>>() {}
        );
        assertThat(multiRoleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(multiRoleResponse.getBody().getCode()).isEqualTo(200);
        assertThat(multiRoleResponse.getBody().getData()).isEqualTo("ADMIN");

        savedAdminAccessToken = adminAccessToken;
    }

    @Test
    @Order(18)
    void shouldAllowAdminToCreateNewDoctorUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        UserCreateRequest request = UserCreateRequest.builder()
                .phone("0988776655")
                .email("doctor.le@hospital.com")
                .password("Doctor@2026")
                .fullName("Bác sĩ Lê Thị Hoa")
                .roleId(2) // DOCTOR
                .gender("female")
                .dateOfBirth(LocalDate.of(1988, 4, 12))
                .address("Hà Nội")
                .isActive(true)
                .build();

        ResponseEntity<ApiResponse<UserResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/users",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
        UserResponse createdUser = response.getBody().getData();
        assertThat(createdUser.getPhone()).isEqualTo("0988776655");
        assertThat(createdUser.getEmail()).isEqualTo("doctor.le@hospital.com");
        assertThat(createdUser.getFullName()).isEqualTo("Bác sĩ Lê Thị Hoa");
        assertThat(createdUser.getRole().getCode()).isEqualTo("DOCTOR");
        createdDoctorUserId = createdUser.getId();
    }

    @Test
    @Order(19)
    void shouldRejectPatientFromCreatingUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedPatientAccessToken);

        UserCreateRequest request = UserCreateRequest.builder()
                .phone("0911223344")
                .fullName("User Bất Hợp Pháp")
                .roleId(1)
                .build();

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/users",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getCode()).isEqualTo(403);
    }

    @Test
    @Order(20)
    void shouldAllowAdminToGetUsersWithPagingAndFiltering() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        ResponseEntity<ApiResponse<PageResponse<UserResponse>>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/users?roleId=2&page=1&size=10",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<PageResponse<UserResponse>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        PageResponse<UserResponse> page = response.getBody().getData();
        assertThat(page.getItems()).isNotEmpty();
        assertThat(page.getItems()).allMatch(u -> "DOCTOR".equals(u.getRole().getCode()));
    }

    @Test
    @Order(21)
    void shouldAllowAdminToGetUserDetailById() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        ResponseEntity<ApiResponse<UserResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/users/" + createdDoctorUserId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getId()).isEqualTo(createdDoctorUserId);
        assertThat(response.getBody().getData().getFullName()).isEqualTo("Bác sĩ Lê Thị Hoa");
    }

    @Test
    @Order(22)
    void shouldAllowAdminToUpdateUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .fullName("Bác sĩ Lê Thị Hoa (CKII)")
                .email("doctor.le.updated@hospital.com")
                .roleId(2)
                .gender("female")
                .dateOfBirth(LocalDate.of(1988, 4, 12))
                .address("Hà Đông, Hà Nội")
                .isActive(true)
                .build();

        ResponseEntity<ApiResponse<UserResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/users/" + createdDoctorUserId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getFullName()).isEqualTo("Bác sĩ Lê Thị Hoa (CKII)");
        assertThat(response.getBody().getData().getEmail()).isEqualTo("doctor.le.updated@hospital.com");
    }

    @Test
    @Order(23)
    void shouldAllowAdminToLockAndUnlockUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        // Khóa tài khoản
        UpdateUserStatusRequest lockRequest = UpdateUserStatusRequest.builder()
                .isActive(false)
                .reason("Tạm khóa để kiểm tra")
                .build();

        ResponseEntity<ApiResponse<UserResponse>> lockResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/users/" + createdDoctorUserId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(lockRequest, headers),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(lockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lockResponse.getBody().getData().getIsActive()).isFalse();

        // Mở khóa lại
        UpdateUserStatusRequest unlockRequest = UpdateUserStatusRequest.builder()
                .isActive(true)
                .reason("Mở khóa sau kiểm tra")
                .build();

        ResponseEntity<ApiResponse<UserResponse>> unlockResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/users/" + createdDoctorUserId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(unlockRequest, headers),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(unlockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unlockResponse.getBody().getData().getIsActive()).isTrue();
    }

    @Test
    @Order(24)
    void shouldAllowUserToViewAndSelfUpdateProfile() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedPatientAccessToken);

        // 1. Xem profile cá nhân
        ResponseEntity<ApiResponse<UserResponse>> profileResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/users/profile",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(profileResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(profileResponse.getBody().getData().getPhone()).isEqualTo("0912345678");

        // 2. Cập nhật profile cá nhân
        UserProfileUpdateRequest updateProfile = UserProfileUpdateRequest.builder()
                .fullName("Nguyễn Văn An (Đã Sửa)")
                .gender("male")
                .dateOfBirth(LocalDate.of(1995, 6, 20))
                .address("Ba Đình, Hà Nội")
                .build();

        ResponseEntity<ApiResponse<UserResponse>> updateResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/users/profile",
                HttpMethod.PUT,
                new HttpEntity<>(updateProfile, headers),
                new ParameterizedTypeReference<ApiResponse<UserResponse>>() {}
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().getData().getFullName()).isEqualTo("Nguyễn Văn An (Đã Sửa)");
        assertThat(updateResponse.getBody().getData().getAddress()).isEqualTo("Ba Đình, Hà Nội");
    }

    @Test
    @Order(25)
    void shouldAllowUserToChangePasswordSuccessfully() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedPatientAccessToken);

        ChangePasswordRequest changePasswordRequest = ChangePasswordRequest.builder()
                .currentPassword("Password@123")
                .newPassword("NewPassword@2026")
                .confirmPassword("NewPassword@2026")
                .build();

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/users/change-password",
                HttpMethod.PUT,
                new HttpEntity<>(changePasswordRequest, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).contains("thành công");
    }

    @Test
    @Order(26)
    void shouldAllowPublicAccessToActiveSpecialties() {
        ResponseEntity<ApiResponse<List<SpecialtyResponse>>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/specialties/active",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<SpecialtyResponse>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
    }

    @Test
    @Order(27)
    void shouldAllowAdminToCreateSpecialtyWithAutoGeneratedSlug() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        SpecialtyCreateRequest request = SpecialtyCreateRequest.builder()
                .name("Khoa Răng Hàm Mặt")
                .description("Khám và điều trị các bệnh về răng, hàm và phẫu thuật thẩm mỹ khuôn mặt")
                .isActive(true)
                .build();

        ResponseEntity<ApiResponse<SpecialtyResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/specialties",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<ApiResponse<SpecialtyResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        SpecialtyResponse createdSpecialty = response.getBody().getData();
        assertThat(createdSpecialty.getName()).isEqualTo("Khoa Răng Hàm Mặt");
        assertThat(createdSpecialty.getSlug()).isEqualTo("khoa-rang-ham-mat");
        assertThat(createdSpecialty.getIsActive()).isTrue();
        createdSpecialtyId = createdSpecialty.getId();
    }

    @Test
    @Order(28)
    void shouldRejectDuplicateSpecialtyName() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        SpecialtyCreateRequest duplicateRequest = SpecialtyCreateRequest.builder()
                .name("Khoa Răng Hàm Mặt")
                .description("Trùng tên")
                .build();

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/specialties",
                HttpMethod.POST,
                new HttpEntity<>(duplicateRequest, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(3002); // SPECIALTY_NAME_EXISTS
    }

    @Test
    @Order(29)
    void shouldAllowPublicAccessToSpecialtyBySlug() {
        ResponseEntity<ApiResponse<SpecialtyResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/specialties/slug/khoa-rang-ham-mat",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<SpecialtyResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getName()).isEqualTo("Khoa Răng Hàm Mặt");
    }

    @Test
    @Order(30)
    void shouldAllowAdminToUpdateSpecialtyAndToggleStatus() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        // 1. Cập nhật tên và mô tả
        SpecialtyUpdateRequest updateRequest = SpecialtyUpdateRequest.builder()
                .name("Khoa Răng - Hàm - Mặt (Kỹ thuật cao)")
                .slug("khoa-rang-ham-mat-ktc")
                .description("Mô tả cập nhật")
                .isActive(true)
                .build();

        ResponseEntity<ApiResponse<SpecialtyResponse>> updateResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/specialties/" + createdSpecialtyId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                new ParameterizedTypeReference<ApiResponse<SpecialtyResponse>>() {}
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().getData().getName()).isEqualTo("Khoa Răng - Hàm - Mặt (Kỹ thuật cao)");
        assertThat(updateResponse.getBody().getData().getSlug()).isEqualTo("khoa-rang-ham-mat-ktc");

        // 2. Tắt trạng thái hoạt động (bật/tắt)
        UpdateSpecialtyStatusRequest statusRequest = UpdateSpecialtyStatusRequest.builder()
                .isActive(false)
                .build();

        ResponseEntity<ApiResponse<SpecialtyResponse>> statusResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/specialties/" + createdSpecialtyId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(statusRequest, headers),
                new ParameterizedTypeReference<ApiResponse<SpecialtyResponse>>() {}
        );

        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResponse.getBody().getData().getIsActive()).isFalse();
    }

    @Test
    @Order(31)
    void shouldAllowAdminToDeleteSpecialty() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        ResponseEntity<ApiResponse<Void>> deleteResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/specialties/" + createdSpecialtyId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Kiểm tra gọi lại chi tiết thì báo NOT_FOUND
        ResponseEntity<ApiResponse<Void>> notFoundResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/specialties/" + createdSpecialtyId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(32)
    void shouldAllowAdminToCreateNewExaminationRoom() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        ExaminationRoomCreateRequest request = ExaminationRoomCreateRequest.builder()
                .roomNumber("P.301")
                .building("Khu B - Tầng 3")
                .isActive(true)
                .build();

        ResponseEntity<ApiResponse<ExaminationRoomResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/examination-rooms",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<ApiResponse<ExaminationRoomResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        ExaminationRoomResponse created = response.getBody().getData();
        assertThat(created.getRoomNumber()).isEqualTo("P.301");
        assertThat(created.getBuilding()).isEqualTo("Khu B - Tầng 3");
        assertThat(created.getIsActive()).isTrue();
        createdRoomId = created.getId();
    }

    @Test
    @Order(33)
    void shouldRejectDuplicateRoomNumberInSameBuilding() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        ExaminationRoomCreateRequest duplicate = ExaminationRoomCreateRequest.builder()
                .roomNumber("P.301")
                .building("Khu B - Tầng 3")
                .build();

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/examination-rooms",
                HttpMethod.POST,
                new HttpEntity<>(duplicate, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(4002); // ROOM_NUMBER_EXISTS
    }

    @Test
    @Order(34)
    void shouldAllowStaffToGetBuildings() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        ResponseEntity<ApiResponse<List<String>>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/examination-rooms/buildings",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<List<String>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).contains("Khu B - Tầng 3");
    }

    @Test
    @Order(35)
    void shouldAllowStaffToGetActiveRooms() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        ResponseEntity<ApiResponse<List<ExaminationRoomResponse>>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/examination-rooms/active",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<List<ExaminationRoomResponse>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData()).isNotEmpty();
    }

    @Test
    @Order(36)
    void shouldAllowAdminToUpdateRoomAndToggleStatus() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        // 1. Cập nhật thông tin phòng
        ExaminationRoomUpdateRequest updateRequest = ExaminationRoomUpdateRequest.builder()
                .roomNumber("P.301-VIP")
                .building("Khu B - Tầng 3 (Khu VIP)")
                .isActive(true)
                .build();

        ResponseEntity<ApiResponse<ExaminationRoomResponse>> updateResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/examination-rooms/" + createdRoomId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                new ParameterizedTypeReference<ApiResponse<ExaminationRoomResponse>>() {}
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().getData().getRoomNumber()).isEqualTo("P.301-VIP");

        // 2. Chuyển phòng sang trạng thái bảo trì (isActive = false)
        UpdateRoomStatusRequest statusRequest = UpdateRoomStatusRequest.builder()
                .isActive(false)
                .build();

        ResponseEntity<ApiResponse<ExaminationRoomResponse>> statusResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/examination-rooms/" + createdRoomId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(statusRequest, headers),
                new ParameterizedTypeReference<ApiResponse<ExaminationRoomResponse>>() {}
        );

        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statusResponse.getBody().getData().getIsActive()).isFalse();
    }

    @Test
    @Order(37)
    void shouldAllowAdminToDeleteRoom() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        ResponseEntity<ApiResponse<Void>> deleteResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/examination-rooms/" + createdRoomId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Gọi lại thì phải báo NOT_FOUND
        ResponseEntity<ApiResponse<Void>> notFoundResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/examination-rooms/" + createdRoomId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(38)
    void shouldAllowPublicAccessToTimeSlots() {
        ResponseEntity<ApiResponse<List<TimeSlotResponse>>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/time-slots",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<List<TimeSlotResponse>>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(200);
    }

    @Test
    @Order(39)
    void shouldAllowAdminToCreateNewTimeSlot() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        TimeSlotCreateRequest request = TimeSlotCreateRequest.builder()
                .startTime(LocalTime.of(7, 30))
                .endTime(LocalTime.of(8, 0))
                .build();

        ResponseEntity<ApiResponse<TimeSlotResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/time-slots",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<ApiResponse<TimeSlotResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        TimeSlotResponse created = response.getBody().getData();
        assertThat(created.getStartTime()).isEqualTo(LocalTime.of(7, 30));
        assertThat(created.getEndTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(created.getSession()).isEqualTo("morning");
        createdTimeSlotId = created.getId();
    }

    @Test
    @Order(40)
    void shouldRejectTimeSlotWithInvalidTimeOrder() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        TimeSlotCreateRequest invalidRequest = TimeSlotCreateRequest.builder()
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(8, 0)) // start > end
                .build();

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/time-slots",
                HttpMethod.POST,
                new HttpEntity<>(invalidRequest, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo(5002); // SLOT_TIME_INVALID
    }

    @Test
    @Order(41)
    void shouldRejectOverlappingTimeSlot() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        TimeSlotCreateRequest overlapRequest = TimeSlotCreateRequest.builder()
                .startTime(LocalTime.of(7, 45))
                .endTime(LocalTime.of(8, 15)) // Overlaps with 07:30 - 08:00
                .build();

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/time-slots",
                HttpMethod.POST,
                new HttpEntity<>(overlapRequest, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().getCode()).isEqualTo(5003); // SLOT_OVERLAPPED_OR_EXISTS
    }

    @Test
    @Order(42)
    void shouldAllowAdminToUpdateTimeSlot() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        TimeSlotUpdateRequest updateRequest = TimeSlotUpdateRequest.builder()
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(7, 30))
                .session("morning")
                .build();

        ResponseEntity<ApiResponse<TimeSlotResponse>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/time-slots/" + createdTimeSlotId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers),
                new ParameterizedTypeReference<ApiResponse<TimeSlotResponse>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getStartTime()).isEqualTo(LocalTime.of(7, 0));
        assertThat(response.getBody().getData().getEndTime()).isEqualTo(LocalTime.of(7, 30));
    }

    @Test
    @Order(43)
    void shouldAllowAdminToDeleteTimeSlot() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(savedAdminAccessToken);

        ResponseEntity<ApiResponse<Void>> response = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/time-slots/" + createdTimeSlotId,
                HttpMethod.DELETE,
                new HttpEntity<>(null, headers),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Gọi lại thì phải báo NOT_FOUND
        ResponseEntity<ApiResponse<Void>> notFoundResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/time-slots/" + createdTimeSlotId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(notFoundResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(44)
    void shouldAllowAdminToCreateDoctorProfile() {
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(savedAdminAccessToken);

        // 1. Tạo chuyên khoa mới để gắn cho bác sĩ
        SpecialtyCreateRequest specialtyReq = SpecialtyCreateRequest.builder()
                .name("Khoa Nội Tổng Hợp")
                .description("Khám nội khoa")
                .isActive(true)
                .build();

        ResponseEntity<ApiResponse<SpecialtyResponse>> specRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/specialties",
                HttpMethod.POST,
                new HttpEntity<>(specialtyReq, adminHeaders),
                new ParameterizedTypeReference<ApiResponse<SpecialtyResponse>>() {}
        );
        assertThat(specRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        doctorTestSpecialtyId = specRes.getBody().getData().getId();

        // 2. Tạo DoctorProfile cho user bác sĩ (createdDoctorUserId đã tạo ở Order 18)
        CreateDoctorProfileRequest createReq = CreateDoctorProfileRequest.builder()
                .userId(createdDoctorUserId)
                .specialtyId(doctorTestSpecialtyId)
                .title("  ThS. BS CKII  ")
                .experienceYears(12)
                .biography("Hơn 12 năm kinh nghiệm điều trị nội khoa.")
                .consultationFee(new BigDecimal("350000.00"))
                .isAvailable(true)
                .build();

        ResponseEntity<ApiResponse<DoctorProfileAdminResponse>> res = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles",
                HttpMethod.POST,
                new HttpEntity<>(createReq, adminHeaders),
                new ParameterizedTypeReference<ApiResponse<DoctorProfileAdminResponse>>() {}
        );

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        DoctorProfileAdminResponse created = res.getBody().getData();
        assertThat(created.getTitle()).isEqualTo("ThS. BS CKII");
        assertThat(created.getConsultationFee()).isEqualByComparingTo("350000.00");
        assertThat(created.getUser().getId()).isEqualTo(createdDoctorUserId);
        assertThat(created.getSpecialty().getId()).isEqualTo(doctorTestSpecialtyId);
        createdDoctorProfileId = created.getId();
    }

    @Test
    @Order(45)
    void shouldRejectInvalidDoctorProfileInput() {
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(savedAdminAccessToken);

        CreateDoctorProfileRequest invalidReq = CreateDoctorProfileRequest.builder()
                .userId(createdDoctorUserId)
                .specialtyId(doctorTestSpecialtyId)
                .title("A".repeat(51)) // > 50 ký tự
                .consultationFee(new BigDecimal("-100.00")) // âm
                .build();

        ResponseEntity<ApiResponse<Object>> res = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles",
                HttpMethod.POST,
                new HttpEntity<>(invalidReq, adminHeaders),
                new ParameterizedTypeReference<ApiResponse<Object>>() {}
        );

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody().getCode()).isEqualTo(422);
    }

    @Test
    @Order(46)
    void shouldRejectPatientFromCreatingOrUpdatingDoctorProfile() {
        HttpHeaders patientHeaders = new HttpHeaders();
        patientHeaders.setBearerAuth(savedPatientAccessToken);

        CreateDoctorProfileRequest createReq = CreateDoctorProfileRequest.builder()
                .userId(createdDoctorUserId)
                .specialtyId(doctorTestSpecialtyId)
                .consultationFee(new BigDecimal("200000.00"))
                .build();

        ResponseEntity<ApiResponse<Void>> postRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles",
                HttpMethod.POST,
                new HttpEntity<>(createReq, patientHeaders),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(postRes.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        UpdateDoctorProfileRequest putReq = UpdateDoctorProfileRequest.builder()
                .specialtyId(doctorTestSpecialtyId)
                .consultationFee(new BigDecimal("200000.00"))
                .build();

        ResponseEntity<ApiResponse<Void>> putRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles/" + createdDoctorProfileId,
                HttpMethod.PUT,
                new HttpEntity<>(putReq, patientHeaders),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(putRes.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(47)
    void shouldAllowPublicToGetDoctorProfilesWithoutSensitiveUserFields() {
        // Gọi không có token (Public)
        ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> res = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles?keyword=ThS",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<PageResponse<Map<String, Object>>>>() {}
        );

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> items = res.getBody().getData().getItems();
        assertThat(items).isNotEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) items.get(0).get("user");
        assertThat(userMap).containsKeys("id", "fullName", "gender");
        assertThat(userMap).doesNotContainKeys("passwordHash", "address", "dateOfBirth", "phone", "email");
    }

    @Test
    @Order(48)
    void shouldAllowDoctorToGetAndUpdateOwnProfileViaMeRoute() {
        // 1. Đăng nhập tài khoản bác sĩ
        LoginRequest doctorLogin = LoginRequest.builder()
                .identifier("0988776655")
                .password("Doctor@2026")
                .build();

        ResponseEntity<ApiResponse<LoginResponse>> loginRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(doctorLogin),
                new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {}
        );
        assertThat(loginRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        savedDoctorAccessToken = loginRes.getBody().getData().getAccessToken();

        HttpHeaders doctorHeaders = new HttpHeaders();
        doctorHeaders.setBearerAuth(savedDoctorAccessToken);

        // 2. GET /api/v1/doctor-profiles/me (chứng minh không conflict với /{id})
        ResponseEntity<ApiResponse<DoctorProfileAdminResponse>> meRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles/me",
                HttpMethod.GET,
                new HttpEntity<>(doctorHeaders),
                new ParameterizedTypeReference<ApiResponse<DoctorProfileAdminResponse>>() {}
        );
        assertThat(meRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(meRes.getBody().getData().getId()).isEqualTo(createdDoctorProfileId);

        // 3. PUT /api/v1/doctor-profiles/me (tự cập nhật biography)
        UpdateMyDoctorProfileRequest updateBio = UpdateMyDoctorProfileRequest.builder()
                .biography("Tiểu sử mới cập nhật bởi chính bác sĩ.")
                .build();

        ResponseEntity<ApiResponse<DoctorProfileAdminResponse>> updateMeRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles/me",
                HttpMethod.PUT,
                new HttpEntity<>(updateBio, doctorHeaders),
                new ParameterizedTypeReference<ApiResponse<DoctorProfileAdminResponse>>() {}
        );
        assertThat(updateMeRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateMeRes.getBody().getData().getBiography()).isEqualTo("Tiểu sử mới cập nhật bởi chính bác sĩ.");
    }

    @Test
    @Order(49)
    void shouldHideUnavailableDoctorFromPublicButShowToAdmin() {
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(savedAdminAccessToken);

        // 1. Tắt isAvailable = false
        UpdateDoctorAvailabilityRequest availReq = UpdateDoctorAvailabilityRequest.builder()
                .isAvailable(false)
                .build();

        ResponseEntity<ApiResponse<DoctorProfileAdminResponse>> availRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles/" + createdDoctorProfileId + "/availability",
                HttpMethod.PUT,
                new HttpEntity<>(availReq, adminHeaders),
                new ParameterizedTypeReference<ApiResponse<DoctorProfileAdminResponse>>() {}
        );
        assertThat(availRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(availRes.getBody().getData().getIsAvailable()).isFalse();

        // 2. Public gọi GET /{id} -> phải trả 404
        ResponseEntity<ApiResponse<Void>> publicGetById = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles/" + createdDoctorProfileId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(publicGetById.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // 3. Public gọi GET / -> không xuất hiện trong kết quả
        ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> publicList = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<PageResponse<Map<String, Object>>>>() {}
        );
        assertThat(publicList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicList.getBody().getData().getItems()).isEmpty();

        // 4. Admin gọi GET / -> vẫn thấy đầy đủ
        ResponseEntity<ApiResponse<PageResponse<DoctorProfileAdminResponse>>> adminList = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/doctor-profiles",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<ApiResponse<PageResponse<DoctorProfileAdminResponse>>>() {}
        );
        assertThat(adminList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminList.getBody().getData().getItems()).hasSize(1);
    }

    @Test
    @Order(50)
    void shouldAllowAdminToCreateMedicalServicesBothSpecialtyAndGeneral() {
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(savedAdminAccessToken);

        // 1. Tạo dịch vụ gắn chuyên khoa (doctorTestSpecialtyId đang active)
        CreateMedicalServiceRequest specialtyServiceReq = CreateMedicalServiceRequest.builder()
                .specialtyId(doctorTestSpecialtyId)
                .name("Khám Nội tổng quát chuyên sâu")
                .price(new BigDecimal("200000.00"))
                .estimatedDurationMin(25)
                .description("Khám lâm sàng và tư vấn")
                .isActive(true)
                .build();

        ResponseEntity<ApiResponse<MedicalServiceResponse>> res1 = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/medical-services",
                HttpMethod.POST,
                new HttpEntity<>(specialtyServiceReq, adminHeaders),
                new ParameterizedTypeReference<ApiResponse<MedicalServiceResponse>>() {}
        );
        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res1.getBody().getData().getSpecialty()).isNotNull();
        createdSpecialtyServiceId = res1.getBody().getData().getId();

        // 2. Tạo dịch vụ dùng chung (specialtyId = null)
        CreateMedicalServiceRequest generalServiceReq = CreateMedicalServiceRequest.builder()
                .specialtyId(null)
                .name("Xét nghiệm máu tổng quát")
                .price(new BigDecimal("150000.00"))
                .estimatedDurationMin(15)
                .description("Dịch vụ dùng chung")
                .isActive(true)
                .build();

        ResponseEntity<ApiResponse<MedicalServiceResponse>> res2 = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/medical-services",
                HttpMethod.POST,
                new HttpEntity<>(generalServiceReq, adminHeaders),
                new ParameterizedTypeReference<ApiResponse<MedicalServiceResponse>>() {}
        );
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res2.getBody().getData().getSpecialty()).isNull();
        createdGeneralServiceId = res2.getBody().getData().getId();
    }

    @Test
    @Order(51)
    void shouldRejectInvalidMedicalServiceInput() {
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(savedAdminAccessToken);

        CreateMedicalServiceRequest invalidReq = CreateMedicalServiceRequest.builder()
                .name("")
                .price(new BigDecimal("-50.00"))
                .estimatedDurationMin(2) // < 5
                .build();

        ResponseEntity<ApiResponse<Object>> res = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/medical-services",
                HttpMethod.POST,
                new HttpEntity<>(invalidReq, adminHeaders),
                new ParameterizedTypeReference<ApiResponse<Object>>() {}
        );

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody().getCode()).isEqualTo(422);
    }

    @Test
    @Order(52)
    void shouldRejectPatientAndDoctorFromModifyingMedicalServices() {
        HttpHeaders patientHeaders = new HttpHeaders();
        patientHeaders.setBearerAuth(savedPatientAccessToken);

        HttpHeaders doctorHeaders = new HttpHeaders();
        doctorHeaders.setBearerAuth(savedDoctorAccessToken);

        CreateMedicalServiceRequest createReq = CreateMedicalServiceRequest.builder()
                .name("Dịch vụ trái phép")
                .price(new BigDecimal("100000.00"))
                .build();

        // PATIENT gọi POST -> 403
        ResponseEntity<ApiResponse<Void>> postRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/medical-services",
                HttpMethod.POST,
                new HttpEntity<>(createReq, patientHeaders),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(postRes.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // DOCTOR gọi PUT /status -> 403
        UpdateMedicalServiceStatusRequest statusReq = UpdateMedicalServiceStatusRequest.builder()
                .isActive(false)
                .build();
        ResponseEntity<ApiResponse<Void>> statusRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/medical-services/" + createdGeneralServiceId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(statusReq, doctorHeaders),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );
        assertThat(statusRes.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(53)
    void shouldIncludeBothGeneralAndSpecialtyServicesViaLeftJoinForPublic() {
        ResponseEntity<ApiResponse<PageResponse<MedicalServiceResponse>>> res = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/medical-services?page=0&size=10&sort=price,asc",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<PageResponse<MedicalServiceResponse>>>() {}
        );

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<MedicalServiceResponse> items = res.getBody().getData().getItems();
        assertThat(items).hasSize(2);
        assertThat(items).extracting(MedicalServiceResponse::getId)
                .containsExactly(createdGeneralServiceId, createdSpecialtyServiceId);
    }

    @Test
    @Order(54)
    void shouldHideServiceWhenParentSpecialtyIsDisabledForPublicButVisibleToAdmin() {
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(savedAdminAccessToken);

        // 1. Tắt chuyên khoa cha (doctorTestSpecialtyId)
        UpdateSpecialtyStatusRequest disableSpec = UpdateSpecialtyStatusRequest.builder()
                .isActive(false)
                .build();
        ResponseEntity<ApiResponse<SpecialtyResponse>> specRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/specialties/" + doctorTestSpecialtyId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(disableSpec, adminHeaders),
                new ParameterizedTypeReference<ApiResponse<SpecialtyResponse>>() {}
        );
        assertThat(specRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 2. Public gọi GET / -> chỉ còn dịch vụ dùng chung (specialty_id = NULL)
        ResponseEntity<ApiResponse<PageResponse<MedicalServiceResponse>>> publicRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/medical-services",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<PageResponse<MedicalServiceResponse>>>() {}
        );
        assertThat(publicRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicRes.getBody().getData().getItems())
                .extracting(MedicalServiceResponse::getId)
                .containsExactly(createdGeneralServiceId);

        // 3. Admin gọi GET / -> vẫn thấy đủ cả 2 dịch vụ
        ResponseEntity<ApiResponse<PageResponse<MedicalServiceResponse>>> adminRes = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/medical-services",
                HttpMethod.GET,
                new HttpEntity<>(adminHeaders),
                new ParameterizedTypeReference<ApiResponse<PageResponse<MedicalServiceResponse>>>() {}
        );
        assertThat(adminRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminRes.getBody().getData().getItems()).hasSize(2);
    }

    @Test
    @Order(55)
    void shouldRejectActivatingServiceWhenParentSpecialtyIsInactive() {
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(savedAdminAccessToken);

        UpdateMedicalServiceStatusRequest activateReq = UpdateMedicalServiceStatusRequest.builder()
                .isActive(true)
                .build();

        ResponseEntity<ApiResponse<Void>> res = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/medical-services/" + createdSpecialtyServiceId + "/status",
                HttpMethod.PUT,
                new HttpEntity<>(activateReq, adminHeaders),
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getCode()).isEqualTo(6004); // SPECIALTY_INACTIVE
    }

    private String getLocalServerUrl() {
        return String.format("http://localhost:%d/", port);
    }
}
