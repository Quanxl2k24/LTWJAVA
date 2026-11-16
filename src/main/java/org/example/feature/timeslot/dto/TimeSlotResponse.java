package org.example.feature.timeslot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết khung giờ khám")
public class TimeSlotResponse {

    @Schema(description = "ID định danh khung giờ", example = "1")
    private Integer id;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Thời gian bắt đầu khám (HH:mm)", example = "08:00")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Thời gian kết thúc khám (HH:mm)", example = "08:30")
    private LocalTime endTime;

    @Schema(description = "Buổi khám trong ngày (morning: Sáng, afternoon: Chiều, evening: Tối)", example = "morning", allowableValues = {"morning", "afternoon", "evening"})
    private String session;

    @Schema(description = "Chuỗi hiển thị trực quan cho người dùng", example = "08:00 - 08:30")
    public String getFormattedSlot() {
        return (startTime != null && endTime != null)
                ? String.format("%02d:%02d - %02d:%02d", startTime.getHour(), startTime.getMinute(), endTime.getHour(), endTime.getMinute())
                : "";
    }
}

