package org.example.feature.medicalservice;

import org.example.common.exception.AppException;
import org.example.common.exception.ErrorCode;
import org.example.common.security.RoleConstant;
import org.example.common.security.UserPrincipal;
import org.example.feature.medicalservice.dto.*;
import org.example.feature.medicalservice.model.MedicalService;
import org.example.feature.medicalservice.repository.MedicalServiceRepository;
import org.example.feature.specialty.model.Specialty;
import org.example.feature.specialty.repository.SpecialtyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalServiceServiceImplTest {

    @Mock
    private MedicalServiceRepository medicalServiceRepository;

    @Mock
    private SpecialtyRepository specialtyRepository;

    @Spy
    private MedicalServiceMapper mapper = new MedicalServiceMapper();

    @InjectMocks
    private MedicalServiceServiceImpl medicalServiceService;

    private Specialty activeSpecialty;
    private Specialty inactiveSpecialty;

    @BeforeEach
    void setUp() {
        activeSpecialty = Specialty.builder()
                .id(1)
                .name("Khoa Tim Mạch")
                .slug("khoa-tim-mach")
                .isActive(true)
                .build();

        inactiveSpecialty = Specialty.builder()
                .id(2)
                .name("Khoa Tạm Ngưng")
                .slug("khoa-tam-ngung")
                .isActive(false)
                .build();
    }

    @Test
    void create_ShouldSuccess_WhenValidSpecialtyProvided() {
        CreateMedicalServiceRequest request = CreateMedicalServiceRequest.builder()
                .specialtyId(1)
                .name("  Siêu âm tim  ")
                .price(new BigDecimal("250000.00"))
                .estimatedDurationMin(20)
                .description("  Mô tả dịch vụ  ")
                .isActive(true)
                .build();

        when(specialtyRepository.findById(1)).thenReturn(Optional.of(activeSpecialty));
        when(medicalServiceRepository.existsByNameIgnoreCaseAndSpecialtyId("Siêu âm tim", 1)).thenReturn(false);
        when(medicalServiceRepository.save(any(MedicalService.class))).thenAnswer(inv -> {
            MedicalService s = inv.getArgument(0);
            s.setId(10);
            return s;
        });

        MedicalServiceResponse response = medicalServiceService.create(request);

        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getName()).isEqualTo("Siêu âm tim");
        assertThat(response.getDescription()).isEqualTo("Mô tả dịch vụ");
        assertThat(response.getPrice()).isEqualByComparingTo("250000.00");
        assertThat(response.getSpecialty()).isNotNull();
        assertThat(response.getSpecialty().getId()).isEqualTo(1);
    }

    @Test
    void create_ShouldSuccess_WhenGeneralServiceWithNullSpecialty() {
        CreateMedicalServiceRequest request = CreateMedicalServiceRequest.builder()
                .specialtyId(null)
                .name("Khám sức khỏe tổng quát")
                .price(new BigDecimal("500000.00"))
                .build();

        when(medicalServiceRepository.existsByNameIgnoreCaseAndSpecialtyIsNull("Khám sức khỏe tổng quát")).thenReturn(false);
        when(medicalServiceRepository.save(any(MedicalService.class))).thenAnswer(inv -> {
            MedicalService s = inv.getArgument(0);
            s.setId(11);
            return s;
        });

        MedicalServiceResponse response = medicalServiceService.create(request);

        assertThat(response.getId()).isEqualTo(11);
        assertThat(response.getSpecialty()).isNull();
        assertThat(response.getEstimatedDurationMin()).isEqualTo(15);
    }

    @Test
    void create_ShouldThrow404_WhenSpecialtyNotFound() {
        CreateMedicalServiceRequest request = CreateMedicalServiceRequest.builder()
                .specialtyId(999)
                .name("Dịch vụ test")
                .price(new BigDecimal("100000.00"))
                .build();

        when(specialtyRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medicalServiceService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPECIALTY_NOT_FOUND);
    }

    @Test
    void create_ShouldThrow400_WhenSpecialtyInactive() {
        CreateMedicalServiceRequest request = CreateMedicalServiceRequest.builder()
                .specialtyId(2)
                .name("Dịch vụ test")
                .price(new BigDecimal("100000.00"))
                .build();

        when(specialtyRepository.findById(2)).thenReturn(Optional.of(inactiveSpecialty));

        assertThatThrownBy(() -> medicalServiceService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPECIALTY_INACTIVE);
    }

    @Test
    void create_ShouldThrow409_WhenDuplicateNameInSameSpecialty() {
        CreateMedicalServiceRequest request = CreateMedicalServiceRequest.builder()
                .specialtyId(1)
                .name("Siêu âm tim")
                .price(new BigDecimal("250000.00"))
                .build();

        when(specialtyRepository.findById(1)).thenReturn(Optional.of(activeSpecialty));
        when(medicalServiceRepository.existsByNameIgnoreCaseAndSpecialtyId("Siêu âm tim", 1)).thenReturn(true);

        assertThatThrownBy(() -> medicalServiceService.create(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEDICAL_SERVICE_ALREADY_EXISTS);
    }

    @Test
    void updateStatus_ShouldThrow400_WhenActivatingServiceWithInactiveSpecialty() {
        MedicalService existing = MedicalService.builder()
                .id(15)
                .name("Nội soi")
                .specialty(inactiveSpecialty)
                .price(new BigDecimal("400000.00"))
                .isActive(false)
                .build();

        UpdateMedicalServiceStatusRequest request = UpdateMedicalServiceStatusRequest.builder()
                .isActive(true)
                .build();

        when(medicalServiceRepository.findWithSpecialtyById(15)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> medicalServiceService.updateStatus(15, request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.SPECIALTY_INACTIVE);
    }

    @Test
    void getAll_ShouldThrow400_WhenMinPriceGreaterThanMaxPrice() {
        MedicalServiceFilterRequest filter = MedicalServiceFilterRequest.builder()
                .minPrice(new BigDecimal("500000.00"))
                .maxPrice(new BigDecimal("100000.00"))
                .build();

        UserPrincipal admin = UserPrincipal.builder().id(1L).role(RoleConstant.ADMIN).build();

        assertThatThrownBy(() -> medicalServiceService.getAll(filter, PageRequest.of(0, 10), admin))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PRICE_RANGE);
    }
}
