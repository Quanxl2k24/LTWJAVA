package org.example.feature.timeslot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dữ liệu yêu cầu cập nhật khung giờ khám")
public class TimeSlotUpdateRequest {

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Giờ bắt đầu khám (định dạng HH:mm)", example = "08:30", requiredMode = Schema.RequiredMode.REQUIRED, type = "string")
    private LocalTime startTime;

    @NotNull(message = "Thời gian kết thúc không được để trống")
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Giờ kết thúc khám (định dạng HH:mm)", example = "09:00", requiredMode = Schema.RequiredMode.REQUIRED, type = "string")
    private LocalTime endTime;

    @Pattern(regexp = "^$|^morning$|^afternoon$|^evening$", message = "Buổi khám chỉ nhận một trong các giá trị: morning, afternoon, evening")
    @Schema(description = "Buổi trong ngày (morning: Sáng, afternoon: Chiều, evening: Tối)", example = "morning", allowableValues = {"morning", "afternoon", "evening"})
    private String session;
}

