package org.example;

import org.example.common.api.ApiResponse;
import org.example.feature.hello.dto.HelloResponse;
import org.example.feature.sample.dto.SampleCreateRequest;
import org.example.feature.sample.dto.SampleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ApplicationTest {

    @Value(value = "${local.server.port}")
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
    }

    @Test
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
    void shouldReturnStandardErrorWhenValidationFails() {
        // Name is blank, violating @NotBlank
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
    void shouldCatchAppExceptionWithGlobalExceptionHandler() {
        ResponseEntity<ApiResponse<Void>> errorResponse = restTemplate.exchange(
                getLocalServerUrl() + "api/v1/samples/demo-error",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<Void>>() {}
        );

        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorResponse.getBody()).isNotNull();
        assertThat(errorResponse.getBody().getCode()).isEqualTo(400);
    }

    private String getLocalServerUrl() {
        return String.format("http://localhost:%d/", port);
    }
}
