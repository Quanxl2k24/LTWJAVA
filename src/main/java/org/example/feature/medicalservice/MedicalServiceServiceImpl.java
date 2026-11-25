package org.example.feature.medicalservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.api.PageResponse;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.common.security.RoleConstant;
import org.example.common.security.UserPrincipal;
import org.example.feature.medicalservice.dto.*;
import org.example.feature.medicalservice.model.MedicalService;
import org.example.feature.medicalservice.repository.MedicalServiceRepository;
import org.example.feature.medicalservice.repository.MedicalServiceSpecification;
import org.example.feature.specialty.model.Specialty;
import org.example.feature.specialty.repository.SpecialtyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalServiceServiceImpl implements MedicalServiceService {

    private final MedicalServiceRepository medicalServiceRepository;
    private final SpecialtyRepository specialtyRepository;
    private final MedicalServiceMapper mapper;

    @Override
    @Transactional
    public MedicalServiceResponse create(CreateMedicalServiceRequest request) {
        String normalizedName = mapper.trimOrNull(request.getName());
        Specialty specialty = null;

        if (request.getSpecialtyId() != null) {
            specialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_FOUND, "Không tìm thấy chuyên khoa với ID: " + request.getSpecialtyId()));

            if (!Boolean.TRUE.equals(specialty.getIsActive())) {
                throw new AppException(ErrorCode.SPECIALTY_INACTIVE, "Chuyên khoa '" + specialty.getName() + "' hiện đang tạm ngưng hoạt động");
            }

            if (medicalServiceRepository.existsByNameIgnoreCaseAndSpecialtyId(normalizedName, specialty.getId())) {
                throw new AppException(ErrorCode.MEDICAL_SERVICE_ALREADY_EXISTS, "Dịch vụ '" + normalizedName + "' đã tồn tại trong chuyên khoa '" + specialty.getName() + "'");
            }
        } else {
            if (medicalServiceRepository.existsByNameIgnoreCaseAndSpecialtyIsNull(normalizedName)) {
                throw new AppException(ErrorCode.MEDICAL_SERVICE_ALREADY_EXISTS, "Dịch vụ dùng chung '" + normalizedName + "' đã tồn tại trên hệ thống");
            }
        }

        MedicalService service = MedicalService.builder()
                .specialty(specialty)
                .name(normalizedName)
                .price(request.getPrice())
                .estimatedDurationMin(request.getEstimatedDurationMin() != null ? request.getEstimatedDurationMin() : 15)
                .description(mapper.trimOrNull(request.getDescription()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        MedicalService saved = medicalServiceRepository.save(service);
        log.info("Đã tạo mới dịch vụ y tế: ID={}, Name={}, SpecialtyId={}",
                saved.getId(), saved.getName(), specialty != null ? specialty.getId() : null);
        return mapper.toResponse(saved);
    }

    @Override
    public PageResponse<MedicalServiceResponse> getAll(MedicalServiceFilterRequest filter, Pageable pageable, UserPrincipal currentUser) {
        if (filter != null) {
            if (filter.getMinPrice() != null && filter.getMaxPrice() != null
                    && filter.getMinPrice().compareTo(filter.getMaxPrice()) > 0) {
                throw new AppException(ErrorCode.INVALID_PRICE_RANGE);
            }

            if (filter.getSpecialtyId() != null && Boolean.TRUE.equals(filter.getGeneralOnly())) {
                throw new AppException(ErrorCode.CONFLICTING_FILTERS);
            }
        }

        boolean publicOnly = !isAdminOrStaff(currentUser);
        Specification<MedicalService> spec = MedicalServiceSpecification.filterBy(filter, publicOnly);
        Page<MedicalService> pageResult = medicalServiceRepository.findAll(spec, pageable);

        List<MedicalServiceResponse> items = pageResult.getContent().stream()
                .map(mapper::toResponse)
                .toList();

        return PageResponse.<MedicalServiceResponse>builder()
                .items(items)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isLast(pageResult.isLast())
                .build();
    }

    @Override
    public MedicalServiceResponse getById(Integer id, UserPrincipal currentUser) {
        MedicalService service = medicalServiceRepository.findWithSpecialtyById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND, "Không tìm thấy dịch vụ khám với ID: " + id));

        if (!isAdminOrStaff(currentUser)) {
            boolean isServiceActive = Boolean.TRUE.equals(service.getIsActive());
            boolean isSpecialtyValid = service.getSpecialty() == null || Boolean.TRUE.equals(service.getSpecialty().getIsActive());

            if (!isServiceActive || !isSpecialtyValid) {
                throw new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND, "Không tìm thấy dịch vụ khám với ID: " + id);
            }
        }

        return mapper.toResponse(service);
    }

    @Override
    @Transactional
    public MedicalServiceResponse update(Integer id, UpdateMedicalServiceRequest request) {
        MedicalService service = medicalServiceRepository.findWithSpecialtyById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND, "Không tìm thấy dịch vụ khám với ID: " + id));

        String normalizedName = mapper.trimOrNull(request.getName());
        Specialty specialty = null;

        if (request.getSpecialtyId() != null) {
            specialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_FOUND, "Không tìm thấy chuyên khoa với ID: " + request.getSpecialtyId()));

            if (!Boolean.TRUE.equals(specialty.getIsActive())) {
                throw new AppException(ErrorCode.SPECIALTY_INACTIVE, "Chuyên khoa '" + specialty.getName() + "' hiện đang tạm ngưng hoạt động");
            }

            if (medicalServiceRepository.existsByNameIgnoreCaseAndSpecialtyIdAndIdNot(normalizedName, specialty.getId(), id)) {
                throw new AppException(ErrorCode.MEDICAL_SERVICE_ALREADY_EXISTS, "Dịch vụ '" + normalizedName + "' đã tồn tại trong chuyên khoa '" + specialty.getName() + "'");
            }
        } else {
            if (medicalServiceRepository.existsByNameIgnoreCaseAndSpecialtyIsNullAndIdNot(normalizedName, id)) {
                throw new AppException(ErrorCode.MEDICAL_SERVICE_ALREADY_EXISTS, "Dịch vụ dùng chung '" + normalizedName + "' đã tồn tại trên hệ thống");
            }
        }

        service.setSpecialty(specialty);
        service.setName(normalizedName);
        service.setPrice(request.getPrice());
        if (request.getEstimatedDurationMin() != null) {
            service.setEstimatedDurationMin(request.getEstimatedDurationMin());
        }
        service.setDescription(mapper.trimOrNull(request.getDescription()));
        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        MedicalService updated = medicalServiceRepository.save(service);
        log.info("Quản trị viên đã cập nhật dịch vụ y tế ID={}", id);
        return mapper.toResponse(updated);
    }

    @Override
    @Transactional
    public MedicalServiceResponse updateStatus(Integer id, UpdateMedicalServiceStatusRequest request) {
        MedicalService service = medicalServiceRepository.findWithSpecialtyById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND, "Không tìm thấy dịch vụ khám với ID: " + id));

        if (Boolean.TRUE.equals(request.getIsActive())
                && service.getSpecialty() != null
                && !Boolean.TRUE.equals(service.getSpecialty().getIsActive())) {
            throw new AppException(
                    ErrorCode.SPECIALTY_INACTIVE,
                    "Không thể kích hoạt lại dịch vụ vì chuyên khoa '" + service.getSpecialty().getName() + "' hiện đang tạm ngưng hoạt động"
            );
        }

        service.setIsActive(request.getIsActive());
        MedicalService updated = medicalServiceRepository.save(service);
        log.info("Đã cập nhật trạng thái dịch vụ y tế ID={} thành {}", id, request.getIsActive());
        return mapper.toResponse(updated);
    }

    private boolean isAdminOrStaff(UserPrincipal currentUser) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        String role = currentUser.getRole().trim();
        return RoleConstant.ADMIN.equalsIgnoreCase(role) || RoleConstant.STAFF.equalsIgnoreCase(role);
    }
}
