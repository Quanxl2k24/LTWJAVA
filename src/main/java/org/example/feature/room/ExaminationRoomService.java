package org.example.feature.room;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.api.PageResponse;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.feature.room.dto.ExaminationRoomCreateRequest;
import org.example.feature.room.dto.ExaminationRoomResponse;
import org.example.feature.room.dto.ExaminationRoomUpdateRequest;
import org.example.feature.room.dto.UpdateRoomStatusRequest;
import org.example.feature.room.model.ExaminationRoom;
import org.example.feature.room.repository.ExaminationRoomRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExaminationRoomService {

    private final ExaminationRoomRepository examinationRoomRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<ExaminationRoomResponse> getActiveRooms() {
        return examinationRoomRepository.findByIsActiveTrueOrderByRoomNumberAsc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctBuildings() {
        return examinationRoomRepository.findDistinctBuildings();
    }

    @Transactional(readOnly = true)
    public PageResponse<ExaminationRoomResponse> getRooms(
            String keyword,
            String building,
            Boolean isActive,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, sort);

        Page<ExaminationRoom> roomPage = examinationRoomRepository.searchRooms(
                keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                building != null && !building.isBlank() ? building.trim() : null,
                isActive,
                pageable
        );

        List<ExaminationRoomResponse> items = roomPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PageResponse.<ExaminationRoomResponse>builder()
                .items(items)
                .pageNumber(roomPage.getNumber() + 1)
                .pageSize(roomPage.getSize())
                .totalElements(roomPage.getTotalElements())
                .totalPages(roomPage.getTotalPages())
                .isLast(roomPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public ExaminationRoomResponse getRoomById(Integer id) {
        ExaminationRoom room = findRoomByIdOrThrow(id);
        return mapToResponse(room);
    }

    @Transactional
    public ExaminationRoomResponse createRoom(ExaminationRoomCreateRequest request) {
        String roomNumber = request.getRoomNumber().trim();
        String building = (request.getBuilding() != null && !request.getBuilding().isBlank())
                ? request.getBuilding().trim()
                : null;

        boolean exists = building != null
                ? examinationRoomRepository.existsByRoomNumberAndBuilding(roomNumber, building)
                : examinationRoomRepository.existsByRoomNumberAndBuildingIsNull(roomNumber);

        if (exists) {
            String location = building != null ? "khu '" + building + "'" : "hệ thống";
            throw new AppException(ErrorCode.ROOM_NUMBER_EXISTS, "Số phòng '" + roomNumber + "' đã tồn tại trong " + location);
        }

        ExaminationRoom room = ExaminationRoom.builder()
                .roomNumber(roomNumber)
                .building(building)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        ExaminationRoom savedRoom = examinationRoomRepository.save(room);
        log.info("Quản trị viên đã tạo mới phòng khám: ID={}, Room={}, Building={}", savedRoom.getId(), savedRoom.getRoomNumber(), savedRoom.getBuilding());
        return mapToResponse(savedRoom);
    }

    @Transactional
    public ExaminationRoomResponse updateRoom(Integer id, ExaminationRoomUpdateRequest request) {
        ExaminationRoom room = findRoomByIdOrThrow(id);
        String roomNumber = request.getRoomNumber().trim();
        String building = (request.getBuilding() != null && !request.getBuilding().isBlank())
                ? request.getBuilding().trim()
                : null;

        boolean exists = building != null
                ? examinationRoomRepository.existsByRoomNumberAndBuildingAndIdNot(roomNumber, building, id)
                : examinationRoomRepository.existsByRoomNumberAndBuildingIsNullAndIdNot(roomNumber, id);

        if (exists) {
            String location = building != null ? "khu '" + building + "'" : "hệ thống";
            throw new AppException(ErrorCode.ROOM_NUMBER_EXISTS, "Số phòng '" + roomNumber + "' đã trùng với phòng khác trong " + location);
        }

        room.setRoomNumber(roomNumber);
        room.setBuilding(building);
        if (request.getIsActive() != null) {
            room.setIsActive(request.getIsActive());
        }

        ExaminationRoom updatedRoom = examinationRoomRepository.save(room);
        log.info("Quản trị viên đã cập nhật phòng khám ID={}", id);
        return mapToResponse(updatedRoom);
    }

    @Transactional
    public ExaminationRoomResponse updateRoomStatus(Integer id, UpdateRoomStatusRequest request) {
        ExaminationRoom room = findRoomByIdOrThrow(id);
        room.setIsActive(request.getIsActive());

        ExaminationRoom updatedRoom = examinationRoomRepository.save(room);
        String action = Boolean.TRUE.equals(request.getIsActive()) ? "kích hoạt" : "chuyển sang bảo trì";
        log.info("Quản trị viên đã {} phòng khám ID={}", action, id);
        return mapToResponse(updatedRoom);
    }

    @Transactional
    public void deleteRoom(Integer id) {
        ExaminationRoom room = findRoomByIdOrThrow(id);

        try {
            // Kiểm tra bảng doctor_schedules nếu có tồn tại
            Integer tableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = 'doctor_schedules'",
                    Integer.class
            );

            if (tableExists != null && tableExists > 0) {
                Long scheduleCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM doctor_schedules WHERE room_id = ?",
                        Long.class,
                        id
                );
                if (scheduleCount != null && scheduleCount > 0) {
                    log.warn("Không thể xóa phòng khám ID={} do có {} ca trực liên kết", id, scheduleCount);
                    throw new AppException(
                            ErrorCode.CANNOT_DELETE_ROOM_WITH_SCHEDULES,
                            "Không thể xóa phòng khám '" + room.getRoomNumber() + "' vì hiện đang có " + scheduleCount + " ca trực/lịch làm việc liên kết. Vui lòng tắt hoạt động phòng thay vì xóa."
                    );
                }
            }

            examinationRoomRepository.delete(room);
            examinationRoomRepository.flush();
            log.info("Quản trị viên đã xóa phòng khám ID={}", id);
        } catch (DataIntegrityViolationException e) {
            log.warn("Không thể xóa phòng khám ID={} do ràng buộc khóa ngoại: {}", id, e.getMessage());
            throw new AppException(
                    ErrorCode.CANNOT_DELETE_ROOM_WITH_SCHEDULES,
                    "Không thể xóa phòng khám vì đang có dữ liệu liên kết. Vui lòng chuyển trạng thái bảo trì/tắt hoạt động."
            );
        }
    }

    private ExaminationRoom findRoomByIdOrThrow(Integer id) {
        return examinationRoomRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND, "Không tìm thấy phòng khám với ID: " + id));
    }

    private ExaminationRoomResponse mapToResponse(ExaminationRoom room) {
        return ExaminationRoomResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .building(room.getBuilding())
                .isActive(room.getIsActive())
                .build();
    }
}

