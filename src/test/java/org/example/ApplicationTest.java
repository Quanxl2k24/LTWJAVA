package org.example;

import org.example.common.api.ApiResponse;
import org.example.common.api.PageResponse;
import org.example.feature.auth.dto.*;
import org.example.feature.hello.dto.HelloResponse;
import org.example.feature.specialty.dto.*;
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

import java.time.LocalDate;
import java.util.List;

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

    private String getLocalServerUrl() {
        return String.format("http://localhost:%d/", port);
    }
}
