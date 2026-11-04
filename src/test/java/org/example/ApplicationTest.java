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

    private String getLocalServerUrl() {
        return String.format("http://localhost:%d/", port);
    }
}
