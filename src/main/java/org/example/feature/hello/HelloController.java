package org.example.feature.hello;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.common.api.ApiResponse;
import org.example.feature.hello.dto.HelloResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hello")
@Tag(name = "Hello Feature", description = "Sample Hello World endpoint adhering to standardized ApiResponse")
public class HelloController {

    @GetMapping
    @Operation(summary = "Get greeting message", description = "Returns greeting wrapped in standardized ApiResponse")
    public ApiResponse<HelloResponse> hello() {
        return ApiResponse.success("Lấy thông điệp chào mừng thành công", new HelloResponse("Hello World!"));
    }
}

