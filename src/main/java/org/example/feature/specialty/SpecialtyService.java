package org.example.feature.specialty;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.api.PageResponse;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.feature.specialty.dto.SpecialtyCreateRequest;
import org.example.feature.specialty.dto.SpecialtyResponse;
import org.example.feature.specialty.dto.SpecialtyUpdateRequest;
import org.example.feature.specialty.dto.UpdateSpecialtyStatusRequest;
import org.example.feature.specialty.model.Specialty;
import org.example.feature.specialty.repository.SpecialtyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> getActiveSpecialties() {
        return specialtyRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::mapToSpecialtyResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<SpecialtyResponse> getSpecialties(
            String keyword,
            Boolean isActive,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, size, sort);

        Page<Specialty> specialtyPage = specialtyRepository.searchSpecialties(
                keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                isActive,
                pageable
        );

        List<SpecialtyResponse> items = specialtyPage.getContent().stream()
                .map(this::mapToSpecialtyResponse)
                .toList();

        return PageResponse.<SpecialtyResponse>builder()
                .items(items)
                .pageNumber(specialtyPage.getNumber() + 1)
                .pageSize(specialtyPage.getSize())
                .totalElements(specialtyPage.getTotalElements())
                .totalPages(specialtyPage.getTotalPages())
                .isLast(specialtyPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public SpecialtyResponse getSpecialtyById(Integer id) {
        Specialty specialty = findSpecialtyByIdOrThrow(id);
        return mapToSpecialtyResponse(specialty);
    }

    @Transactional(readOnly = true)
    public SpecialtyResponse getSpecialtyBySlug(String slug) {
        Specialty specialty = specialtyRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_FOUND, "Không tìm thấy chuyên khoa với đường dẫn slug: " + slug));
        return mapToSpecialtyResponse(specialty);
    }

    @Transactional
    public SpecialtyResponse createSpecialty(SpecialtyCreateRequest request) {
        String name = request.getName().trim();

        if (specialtyRepository.existsByName(name)) {
            throw new AppException(ErrorCode.SPECIALTY_NAME_EXISTS, "Tên chuyên khoa '" + name + "' đã tồn tại");
        }

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? request.getSlug().trim().toLowerCase()
                : generateSlug(name);

        if (specialtyRepository.existsBySlug(slug)) {
            throw new AppException(ErrorCode.SPECIALTY_SLUG_EXISTS, "Đường dẫn slug '" + slug + "' đã được sử dụng");
        }

        Specialty specialty = Specialty.builder()
                .name(name)
                .slug(slug)
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Specialty savedSpecialty = specialtyRepository.save(specialty);
        log.info("Quản trị viên đã tạo mới chuyên khoa: ID={}, Name={}, Slug={}", savedSpecialty.getId(), savedSpecialty.getName(), savedSpecialty.getSlug());
        return mapToSpecialtyResponse(savedSpecialty);
    }

    @Transactional
    public SpecialtyResponse updateSpecialty(Integer id, SpecialtyUpdateRequest request) {
        Specialty specialty = findSpecialtyByIdOrThrow(id);
        String name = request.getName().trim();

        if (specialtyRepository.existsByNameAndIdNot(name, id)) {
            throw new AppException(ErrorCode.SPECIALTY_NAME_EXISTS, "Tên chuyên khoa '" + name + "' đã trùng với chuyên khoa khác");
        }

        String slug = (request.getSlug() != null && !request.getSlug().isBlank())
                ? request.getSlug().trim().toLowerCase()
                : generateSlug(name);

        if (specialtyRepository.existsBySlugAndIdNot(slug, id)) {
            throw new AppException(ErrorCode.SPECIALTY_SLUG_EXISTS, "Đường dẫn slug '" + slug + "' đã được sử dụng bởi chuyên khoa khác");
        }

        specialty.setName(name);
        specialty.setSlug(slug);
        specialty.setDescription(request.getDescription());
        if (request.getIsActive() != null) {
            specialty.setIsActive(request.getIsActive());
        }

        Specialty updatedSpecialty = specialtyRepository.save(specialty);
        log.info("Quản trị viên đã cập nhật chuyên khoa ID={}", id);
        return mapToSpecialtyResponse(updatedSpecialty);
    }

    @Transactional
    public SpecialtyResponse updateSpecialtyStatus(Integer id, UpdateSpecialtyStatusRequest request) {
        Specialty specialty = findSpecialtyByIdOrThrow(id);
        specialty.setIsActive(request.getIsActive());

        Specialty updatedSpecialty = specialtyRepository.save(specialty);
        String action = Boolean.TRUE.equals(request.getIsActive()) ? "bật hoạt động" : "tắt hoạt động";
        log.info("Quản trị viên đã {} chuyên khoa ID={}", action, id);
        return mapToSpecialtyResponse(updatedSpecialty);
    }

    @Transactional
    public void deleteSpecialty(Integer id) {
        Specialty specialty = findSpecialtyByIdOrThrow(id);

        try {
            // 1. Kiểm tra nếu bảng doctor_profiles có tồn tại trong database (MySQL hoặc H2)
            Integer tableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = 'doctor_profiles'",
                    Integer.class
            );

            if (tableExists != null && tableExists > 0) {
                Long doctorCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM doctor_profiles WHERE specialty_id = ?",
                        Long.class,
                        id
                );
                if (doctorCount != null && doctorCount > 0) {
                    log.warn("Không thể xóa chuyên khoa ID={} do có {} bác sĩ phụ thuộc", id, doctorCount);
                    throw new AppException(
                            ErrorCode.CANNOT_DELETE_SPECIALTY_HAS_DOCTORS,
                            "Không thể xóa chuyên khoa '" + specialty.getName() + "' vì hiện đang có " + doctorCount + " bác sĩ trực thuộc. Vui lòng tắt hoạt động chuyên khoa thay vì xóa."
                    );
                }
            }

            specialtyRepository.delete(specialty);
            specialtyRepository.flush();
            log.info("Quản trị viên đã xóa chuyên khoa ID={}", id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Không thể xóa chuyên khoa ID={} do ràng buộc khóa ngoại: {}", id, e.getMessage());
            throw new AppException(
                    ErrorCode.CANNOT_DELETE_SPECIALTY_HAS_DOCTORS,
                    "Không thể xóa chuyên khoa '" + specialty.getName() + "' vì đang có dữ liệu liên kết (bác sĩ hoặc dịch vụ y tế). Vui lòng tắt hoạt động thay vì xóa."
            );
        }
    }

    private Specialty findSpecialtyByIdOrThrow(Integer id) {
        return specialtyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_FOUND, "Không tìm thấy chuyên khoa với ID: " + id));
    }

    private SpecialtyResponse mapToSpecialtyResponse(Specialty specialty) {
        return SpecialtyResponse.builder()
                .id(specialty.getId())
                .name(specialty.getName())
                .slug(specialty.getSlug())
                .description(specialty.getDescription())
                .isActive(specialty.getIsActive())
                .build();
    }

    public static String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String text = input.replace("đ", "d").replace("Đ", "d");
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String noDiacritics = pattern.matcher(normalized).replaceAll("");
        return noDiacritics.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}

