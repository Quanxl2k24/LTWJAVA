package org.example.feature.timeslot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.feature.timeslot.dto.TimeSlotCreateRequest;
import org.example.feature.timeslot.dto.TimeSlotResponse;
import org.example.feature.timeslot.dto.TimeSlotUpdateRequest;
import org.example.feature.timeslot.model.TimeSlot;
import org.example.feature.timeslot.repository.TimeSlotRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAllTimeSlots(String session) {
        List<TimeSlot> slots;
        if (session != null && !session.isBlank()) {
            slots = timeSlotRepository.findBySessionOrderByStartTimeAsc(session.trim().toLowerCase());
        } else {
            slots = timeSlotRepository.findAllByOrderByStartTimeAsc();
        }

        return slots.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TimeSlotResponse getTimeSlotById(Integer id) {
        TimeSlot slot = findSlotByIdOrThrow(id);
        return mapToResponse(slot);
    }

    @Transactional
    public TimeSlotResponse createTimeSlot(TimeSlotCreateRequest request) {
        LocalTime start = request.getStartTime();
        LocalTime end = request.getEndTime();

        if (!start.isBefore(end)) {
            throw new AppException(ErrorCode.SLOT_TIME_INVALID, "Giờ bắt đầu (" + start + ") phải trước giờ kết thúc (" + end + ")");
        }

        long overlapCount = timeSlotRepository.countOverlappingSlots(start, end, null);
        if (overlapCount > 0) {
            throw new AppException(ErrorCode.SLOT_OVERLAPPED_OR_EXISTS, "Khung giờ " + start + " - " + end + " bị trùng lặp hoặc chồng chéo với khung giờ đã có");
        }

        String session = (request.getSession() != null && !request.getSession().isBlank())
                ? request.getSession().trim().toLowerCase()
                : determineSession(start);

        TimeSlot slot = TimeSlot.builder()
                .startTime(start)
                .endTime(end)
                .session(session)
                .build();

        TimeSlot savedSlot = timeSlotRepository.save(slot);
        log.info("Quản trị viên đã tạo mới khung giờ khám: ID={}, {} - {}, Buổi: {}", savedSlot.getId(), start, end, session);
        return mapToResponse(savedSlot);
    }

    @Transactional
    public TimeSlotResponse updateTimeSlot(Integer id, TimeSlotUpdateRequest request) {
        TimeSlot slot = findSlotByIdOrThrow(id);
        LocalTime start = request.getStartTime();
        LocalTime end = request.getEndTime();

        if (!start.isBefore(end)) {
            throw new AppException(ErrorCode.SLOT_TIME_INVALID, "Giờ bắt đầu (" + start + ") phải trước giờ kết thúc (" + end + ")");
        }

        long overlapCount = timeSlotRepository.countOverlappingSlots(start, end, id);
        if (overlapCount > 0) {
            throw new AppException(ErrorCode.SLOT_OVERLAPPED_OR_EXISTS, "Khung giờ " + start + " - " + end + " bị trùng lặp hoặc chồng chéo với khung giờ khác");
        }

        String session = (request.getSession() != null && !request.getSession().isBlank())
                ? request.getSession().trim().toLowerCase()
                : determineSession(start);

        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setSession(session);

        TimeSlot updatedSlot = timeSlotRepository.save(slot);
        log.info("Quản trị viên đã cập nhật khung giờ khám ID={}", id);
        return mapToResponse(updatedSlot);
    }

    @Transactional
    public void deleteTimeSlot(Integer id) {
        TimeSlot slot = findSlotByIdOrThrow(id);

        try {
            // Kiểm tra bảng doctor_schedules nếu có tồn tại
            Integer tableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = 'doctor_schedules'",
                    Integer.class
            );

            if (tableExists != null && tableExists > 0) {
                Long scheduleCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM doctor_schedules WHERE slot_id = ?",
                        Long.class,
                        id
                );
                if (scheduleCount != null && scheduleCount > 0) {
                    log.warn("Không thể xóa khung giờ ID={} do có {} lịch khám liên kết", id, scheduleCount);
                    throw new AppException(
                            ErrorCode.CANNOT_DELETE_SLOT_IN_USE,
                            "Không thể xóa khung giờ (" + slot.getStartTime() + " - " + slot.getEndTime() + ") vì hiện đang có " + scheduleCount + " ca trực của bác sĩ đã liên kết."
                    );
                }
            }

            timeSlotRepository.delete(slot);
            timeSlotRepository.flush();
            log.info("Quản trị viên đã xóa khung giờ khám ID={}", id);
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể xóa khung giờ ID={} do ràng buộc khóa ngoại: {}", id, e.getMessage());
            throw new AppException(
                    ErrorCode.CANNOT_DELETE_SLOT_IN_USE,
                    "Không thể xóa khung giờ vì đang có lịch làm việc của bác sĩ liên kết."
            );
        }
    }

    private TimeSlot findSlotByIdOrThrow(Integer id) {
        return timeSlotRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SLOT_NOT_FOUND, "Không tìm thấy khung giờ khám với ID: " + id));
    }

    private String determineSession(LocalTime startTime) {
        if (startTime.isBefore(LocalTime.of(12, 0))) {
            return "morning";
        } else if (startTime.isBefore(LocalTime.of(17, 0))) {
            return "afternoon";
        } else {
            return "evening";
        }
    }

    private TimeSlotResponse mapToResponse(TimeSlot slot) {
        return TimeSlotResponse.builder()
                .id(slot.getId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .session(slot.getSession())
                .build();
    }
}

