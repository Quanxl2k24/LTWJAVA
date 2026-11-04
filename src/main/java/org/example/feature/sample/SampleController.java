package org.example.feature.sample;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.common.api.ApiResponse;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.feature.sample.dto.SampleCreateRequest;
import org.example.feature.sample.dto.SampleResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/samples")
@RequiredArgsConstructor
@Tag(name = "Sample Feature", description = "Minh họa cấu trúc Package-by-Feature và chuẩn hóa ApiResponse")
public class SampleController {

    private final SampleService sampleService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các sample")
    public ApiResponse<List<SampleResponse>> getAll() {
        return ApiResponse.success("Lấy danh sách thành công", sampleService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết sample theo ID")
    public ApiResponse<SampleResponse> getById(@PathVariable Long id) {
        return ApiResponse.success("Lấy thông tin chi tiết thành công", sampleService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Tạo mới sample với Bean Validation")
    public ApiResponse<SampleResponse> create(@Valid @RequestBody SampleCreateRequest request) {
        return ApiResponse.success("Tạo mới thành công", sampleService.create(request));
    }

    @GetMapping("/demo-error")
    @Operation(summary = "Demo kiểm tra GlobalExceptionHandler bắt AppException")
    public ApiResponse<Void> demoError() {
        throw new AppException(ErrorCode.BAD_REQUEST, "Đây là lỗi demo được bắt bởi GlobalExceptionHandler");
    }
}

