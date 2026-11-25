package org.example.feature.doctorprofile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.api.PageResponse;
import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.common.security.RoleConstant;
import org.example.common.security.UserPrincipal;
import org.example.feature.auth.model.User;
import org.example.feature.auth.repository.UserRepository;
import org.example.feature.doctorprofile.dto.*;
import org.example.feature.doctorprofile.model.DoctorProfile;
import org.example.feature.doctorprofile.repository.DoctorProfileRepository;
import org.example.feature.doctorprofile.repository.DoctorProfileSpecification;
import org.example.feature.specialty.model.Specialty;
import org.example.feature.specialty.repository.SpecialtyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorProfileServiceImpl implements DoctorProfileService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final SpecialtyRepository specialtyRepository;
    private final DoctorProfileMapper mapper;

    @Override
    @Transactional
    public DoctorProfileAdminResponse create(CreateDoctorProfileRequest request) {
        // 1. Kiểm tra User tồn tại
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với ID: " + request.getUserId()));

        // 2. Kiểm tra tài khoản đang hoạt động
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Tài khoản người dùng hiện đang bị khóa, không thể tạo hồ sơ bác sĩ");
        }

        // 3. Kiểm tra vai trò DOCTOR (không hardcode role_id)
        if (user.getRole() == null || !RoleConstant.DOCTOR.equalsIgnoreCase(user.getRole().getCode())) {
            throw new AppException(ErrorCode.INVALID_USER_ROLE, "Tài khoản được chọn không có vai trò Bác sĩ (DOCTOR)");
        }

        // 4. Kiểm tra chưa có hồ sơ bác sĩ
        if (doctorProfileRepository.existsByUserId(request.getUserId())) {
            throw new AppException(ErrorCode.DOCTOR_PROFILE_ALREADY_EXISTS, "Người dùng ID=" + request.getUserId() + " đã có hồ sơ bác sĩ");
        }

        // 5. Kiểm tra Specialty tồn tại và đang hoạt động
        Specialty specialty = specialtyRepository.findById(request.getSpecialtyId())
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_FOUND, "Không tìm thấy chuyên khoa với ID: " + request.getSpecialtyId()));

        if (!Boolean.TRUE.equals(specialty.getIsActive())) {
            throw new AppException(ErrorCode.SPECIALTY_INACTIVE, "Chuyên khoa '" + specialty.getName() + "' hiện đang tạm ngưng hoạt động");
        }

        DoctorProfile profile = DoctorProfile.builder()
                .user(user)
                .specialty(specialty)
                .title(mapper.normalizeTitle(request.getTitle()))
                .experienceYears(request.getExperienceYears() != null ? request.getExperienceYears() : 0)
                .biography(request.getBiography())
                .consultationFee(request.getConsultationFee() != null ? request.getConsultationFee() : BigDecimal.ZERO)
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .build();

        DoctorProfile saved = doctorProfileRepository.save(profile);
        log.info("Đã tạo mới hồ sơ bác sĩ: ID={}, UserId={}, SpecialtyId={}", saved.getId(), user.getId(), specialty.getId());
        return mapper.toAdminResponse(saved);
    }

    @Override
    public PageResponse<?> getAll(DoctorProfileFilterRequest filter, Pageable pageable, UserPrincipal currentUser) {
        boolean isPrivileged = isAdminOrStaff(currentUser);
        boolean publicOnly = !isPrivileged;

        Specification<DoctorProfile> spec = DoctorProfileSpecification.filterBy(filter, publicOnly);
        Page<DoctorProfile> pageResult = doctorProfileRepository.findAll(spec, pageable);

        if (isPrivileged) {
            List<DoctorProfileAdminResponse> items = pageResult.getContent().stream()
                    .map(mapper::toAdminResponse)
                    .toList();
            return buildPageResponse(pageResult, items);
        } else {
            List<DoctorProfilePublicResponse> items = pageResult.getContent().stream()
                    .map(mapper::toPublicResponse)
                    .toList();
            return buildPageResponse(pageResult, items);
        }
    }

    @Override
    public Object getById(Long id, UserPrincipal currentUser) {
        DoctorProfile profile = doctorProfileRepository.findWithDetailsById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_PROFILE_NOT_FOUND, "Không tìm thấy hồ sơ bác sĩ với ID: " + id));

        if (!isAdminOrStaff(currentUser)) {
            boolean isPubliclyVisible = Boolean.TRUE.equals(profile.getIsAvailable())
                    && profile.getUser() != null && Boolean.TRUE.equals(profile.getUser().getIsActive())
                    && profile.getSpecialty() != null && Boolean.TRUE.equals(profile.getSpecialty().getIsActive());

            if (!isPubliclyVisible) {
                throw new AppException(ErrorCode.DOCTOR_PROFILE_NOT_FOUND, "Không tìm thấy hồ sơ bác sĩ với ID: " + id);
            }
            return mapper.toPublicResponse(profile);
        }

        return mapper.toAdminResponse(profile);
    }

    @Override
    public DoctorProfileAdminResponse getMyProfile(Long currentUserId) {
        DoctorProfile profile = doctorProfileRepository.findWithDetailsByUserId(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_PROFILE_NOT_FOUND, "Không tìm thấy hồ sơ bác sĩ cho tài khoản hiện tại"));
        return mapper.toAdminResponse(profile);
    }

    @Override
    @Transactional
    public DoctorProfileAdminResponse update(Long id, UpdateDoctorProfileRequest request) {
        DoctorProfile profile = doctorProfileRepository.findWithDetailsById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_PROFILE_NOT_FOUND, "Không tìm thấy hồ sơ bác sĩ với ID: " + id));

        // Nếu đổi chuyên khoa hoặc chỉ định chuyên khoa mới, kiểm tra tồn tại và đang hoạt động
        if (profile.getSpecialty() == null || !profile.getSpecialty().getId().equals(request.getSpecialtyId())) {
            Specialty newSpecialty = specialtyRepository.findById(request.getSpecialtyId())
                    .orElseThrow(() -> new AppException(ErrorCode.SPECIALTY_NOT_FOUND, "Không tìm thấy chuyên khoa với ID: " + request.getSpecialtyId()));

            if (!Boolean.TRUE.equals(newSpecialty.getIsActive())) {
                throw new AppException(ErrorCode.SPECIALTY_INACTIVE, "Chuyên khoa '" + newSpecialty.getName() + "' hiện đang tạm ngưng hoạt động");
            }
            profile.setSpecialty(newSpecialty);
        }

        profile.setTitle(mapper.normalizeTitle(request.getTitle()));
        if (request.getExperienceYears() != null) {
            profile.setExperienceYears(request.getExperienceYears());
        }
        profile.setBiography(request.getBiography());
        profile.setConsultationFee(request.getConsultationFee());
        if (request.getIsAvailable() != null) {
            profile.setIsAvailable(request.getIsAvailable());
        }

        DoctorProfile updated = doctorProfileRepository.save(profile);
        log.info("Quản trị viên đã cập nhật hồ sơ bác sĩ ID={}", id);
        return mapper.toAdminResponse(updated);
    }

    @Override
    @Transactional
    public DoctorProfileAdminResponse updateMyProfile(Long currentUserId, UpdateMyDoctorProfileRequest request) {
        DoctorProfile profile = doctorProfileRepository.findWithDetailsByUserId(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_PROFILE_NOT_FOUND, "Không tìm thấy hồ sơ bác sĩ cho tài khoản hiện tại"));

        profile.setBiography(request.getBiography());
        DoctorProfile updated = doctorProfileRepository.save(profile);
        log.info("Bác sĩ userId={} đã tự cập nhật tiểu sử chuyên môn", currentUserId);
        return mapper.toAdminResponse(updated);
    }

    @Override
    @Transactional
    public DoctorProfileAdminResponse updateAvailability(Long id, UpdateDoctorAvailabilityRequest request, UserPrincipal currentUser) {
        DoctorProfile profile = doctorProfileRepository.findWithDetailsById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_PROFILE_NOT_FOUND, "Không tìm thấy hồ sơ bác sĩ với ID: " + id));

        // Nếu người gọi là DOCTOR, chỉ cho phép sửa trạng thái của chính hồ sơ thuộc về mình
        if (currentUser != null && RoleConstant.DOCTOR.equalsIgnoreCase(currentUser.getRole())) {
            if (profile.getUser() == null || !profile.getUser().getId().equals(currentUser.getId())) {
                log.warn("Bác sĩ userId={} cố gắng thay đổi trạng thái hồ sơ bác sĩ khác (profileId={})", currentUser.getId(), id);
                throw new AppException(ErrorCode.FORBIDDEN, "Bạn chỉ được phép thay đổi trạng thái nhận khám của chính mình");
            }
        }

        profile.setIsAvailable(request.getIsAvailable());
        DoctorProfile updated = doctorProfileRepository.save(profile);
        log.info("Đã cập nhật trạng thái nhận khám hồ sơ bác sĩ ID={} thành {}", id, request.getIsAvailable());
        return mapper.toAdminResponse(updated);
    }

    private boolean isAdminOrStaff(UserPrincipal currentUser) {
        if (currentUser == null || currentUser.getRole() == null) {
            return false;
        }
        String role = currentUser.getRole().trim();
        return RoleConstant.ADMIN.equalsIgnoreCase(role) || RoleConstant.STAFF.equalsIgnoreCase(role);
    }

    private <T> PageResponse<T> buildPageResponse(Page<?> pageResult, List<T> items) {
        return PageResponse.<T>builder()
                .items(items)
                .pageNumber(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .isLast(pageResult.isLast())
                .build();
    }
}
