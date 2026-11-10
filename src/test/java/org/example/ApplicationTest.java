package org.example;

import org.example.common.api.ApiResponse;
import org.example.feature.auth.dto.*;
import org.example.feature.hello.dto.HelloResponse;
import org.example.feature.sample.dto.SampleCreateRequest;
import org.example.feature.sample.dto.SampleResponse;
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
    @Order(3)
    void shouldCreateAndRetrieveSampleSuccessfully() {
        SampleCreateRequest request = SampleCreateRequest.builder()
                .name("Test Product")
                .description("Demo description")
                .build();

        ResponseEntity<ApiResponse<SampleResponse>> createResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/samples",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<SampleResponse>>() {}
        );

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getCode()).isEqualTo(200);
        assertThat(createResponse.getBody().getData().getName()).isEqualTo("Test Product");
    }

    @Test
    @Order(4)
    void shouldReturnStandardErrorWhenValidationFails() {
        SampleCreateRequest invalidRequest = SampleCreateRequest.builder()
                .name("")
                .description("Invalid payload")
                .build();

        ResponseEntity<ApiResponse<Object>> errorResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/samples",
                HttpMethod.POST,
                new HttpEntity<>(invalidRequest),
                new ParameterizedTypeReference<ApiResponse<Object>>() {}
        );

        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(errorResponse.getBody()).isNotNull();
        assertThat(errorResponse.getBody().getCode()).isEqualTo(422);
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
    }

    private String getLocalServerUrl() {
        return String.format("http://localhost:%d/", port);
    }
}
