package org.example.feature.doctorprofile;

import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.common.security.RoleConstant;
import org.example.common.security.UserPrincipal;
import org.example.feature.auth.model.Role;
import org.example.feature.auth.model.User;
import org.example.feature.auth.repository.UserRepository;
import org.example.feature.doctorprofile.dto.*;
import org.example.feature.doctorprofile.model.DoctorProfile;
import org.example.feature.doctorprofile.repository.DoctorProfileRepository;
import org.example.feature.specialty.model.Specialty;
import org.example.feature.specialty.repository.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorProfileServiceImplTest {

    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Spy
    private DoctorProfileMapper mapper = new DoctorProfileMapper();

    @InjectMocks
    private DoctorProfileServiceImpl doctorProfileService;

    private Role doctorRole;
    private Role patientRole;
    private User activeDoctorUser;
    private Specialty activeSpecialty;
    private Specialty inactiveSpecialty;

    @BeforeEach
    void setUp() {
        doctorRole = Role.builder().id(2).code(RoleConstant.DOCTOR).name("Bác sĩ").build();
        patientRole = Role.builder().id(4).code(RoleConstant.PATIENT).name("Bệnh nhân").build();

        activeDoctorUser = User.builder()
                .id(10L)
                .fullName("BS. Nguyễn Văn A")
                .phone("0911111111")
                .email("bs.a@hospital.com")
                .gender("male")
                .role(doctorRole)
                .isActive(true)
                .build();

        activeSpecialty = Specialty.builder()
                .id(1)
                .name("Khoa Tim Mạch")
                .slug("khoa-tim-mach")
                .isActive(true)
                .build();

        inactiveSpecialty = Specialty.builder()
                .id(2)
                .name("Khoa Ngưng Hoạt Động")
                .slug("khoa-ngung")
                .isActive(false)
                .build();
    }

    @Test
    void create_ShouldSuccess_WhenValidRequest() {
        CreateDoctorProfileRequest request = CreateDoctorProfileRequest.builder()
                .userId(10L)
                .specialtyId(1)
                .title("  ThS. BS  ")
                .experienceYears(8)
                .biography("Chuyên gia tim mạch")
                .consultationFee(new BigDecimal("350000.00"))
                .isAvailable(true)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(activeDoctorUser));
        when(doctorProfileRepository.existsByUserId(10L)).thenReturn(false);
        when(specialtyRepository.findById(1)).thenReturn(Optional.of(activeSpecialty));
        when(doctorProfileRepository.save(any(DoctorProfile.class))).thenAnswer(inv -> {
            DoctorProfile saved = inv.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        DoctorProfileAdminResponse response = doctorProfileService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getTitle()).isEqualTo("ThS. BS"); // đã trim
        assertThat(response.getConsultationFee()).isEqualByComparingTo("350000.00");
        assertThat(response.getUser().getId()).isEqualTo(10L);
        assertThat(response.getSpecialty().getId()).isEqualTo(1);
    }

    @Test
    void create_ShouldThrow404_WhenUserNotFound() {
        CreateDoctorProfileRequest request = CreateDoctorProfileRequest.builder()
                .userId(999L)
                .specialtyId(1)
                .consultationFee(BigDecimal.ZERO)
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorProfileService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void create_ShouldThrow400_WhenUserNotDoctorRole() {
        User patientUser = User.builder().id(20L).role(patientRole).isActive(true).build();
        CreateDoctorProfileRequest request = CreateDoctorProfileRequest.builder()
                .userId(20L)
                .specialtyId(1)
                .consultationFee(BigDecimal.ZERO)
                .build();

        when(userRepository.findById(20L)).thenReturn(Optional.of(patientUser));

        assertThatThrownBy(() -> doctorProfileService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_USER_ROLE);
    }

    @Test
    void create_ShouldThrow400_WhenUserInactive() {
        User inactiveDoctor = User.builder().id(21L).role(doctorRole).isActive(false).build();
        CreateDoctorProfileRequest request = CreateDoctorProfileRequest.builder()
                .userId(21L)
                .specialtyId(1)
                .consultationFee(BigDecimal.ZERO)
                .build();

        when(userRepository.findById(21L)).thenReturn(Optional.of(inactiveDoctor));

        assertThatThrownBy(() -> doctorProfileService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void create_ShouldThrow409_WhenDoctorProfileAlreadyExists() {
        CreateDoctorProfileRequest request = CreateDoctorProfileRequest.builder()
                .userId(10L)
                .specialtyId(1)
                .consultationFee(BigDecimal.ZERO)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(activeDoctorUser));
        when(doctorProfileRepository.existsByUserId(10L)).thenReturn(true);

        assertThatThrownBy(() -> doctorProfileService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.DOCTOR_PROFILE_ALREADY_EXISTS);
    }

    @Test
    void create_ShouldThrow400_WhenSpecialtyInactive() {
        CreateDoctorProfileRequest request = CreateDoctorProfileRequest.builder()
                .userId(10L)
                .specialtyId(2)
                .consultationFee(BigDecimal.ZERO)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(activeDoctorUser));
        when(doctorProfileRepository.existsByUserId(10L)).thenReturn(false);
        when(specialtyRepository.findById(2)).thenReturn(Optional.of(inactiveSpecialty));

        assertThatThrownBy(() -> doctorProfileService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPECIALTY_INACTIVE);
    }

    @Test
    void updateAvailability_ShouldThrow403_WhenDoctorUpdatesAnotherDoctorsProfile() {
        User doctorB = User.builder().id(99L).role(doctorRole).isActive(true).build();
        DoctorProfile profileOfB = DoctorProfile.builder()
                .id(50L)
                .user(doctorB)
                .specialty(activeSpecialty)
                .isAvailable(true)
                .build();

        UserPrincipal doctorAPrincipal = UserPrincipal.builder()
                .id(10L) // Khác 99L
                .role(RoleConstant.DOCTOR)
                .build();

        UpdateDoctorAvailabilityRequest request = UpdateDoctorAvailabilityRequest.builder()
                .isAvailable(false)
                .build();

        when(doctorProfileRepository.findWithDetailsById(50L)).thenReturn(Optional.of(profileOfB));

        assertThatThrownBy(() -> doctorProfileService.updateAvailability(50L, request, doctorAPrincipal))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void update_ShouldThrow400_WhenNewSpecialtyIsInactive() {
        DoctorProfile existingProfile = DoctorProfile.builder()
                .id(50L)
                .user(activeDoctorUser)
                .specialty(activeSpecialty)
                .consultationFee(new BigDecimal("200000.00"))
                .build();

        UpdateDoctorProfileRequest request = UpdateDoctorProfileRequest.builder()
                .specialtyId(2) // Đổi sang khoa bị tắt
                .consultationFee(new BigDecimal("250000.00"))
                .build();

        when(doctorProfileRepository.findWithDetailsById(50L)).thenReturn(Optional.of(existingProfile));
        when(specialtyRepository.findById(2)).thenReturn(Optional.of(inactiveSpecialty));

        assertThatThrownBy(() -> doctorProfileService.update(50L, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPECIALTY_INACTIVE);
    }
}
