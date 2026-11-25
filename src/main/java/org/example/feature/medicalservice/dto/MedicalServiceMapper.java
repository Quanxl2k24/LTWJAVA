package org.example.feature.medicalservice.dto;

import org.example.feature.medicalservice.model.MedicalService;
import org.example.feature.specialty.model.Specialty;
import org.springframework.stereotype.Component;

@Component
public class MedicalServiceMapper {

    public String trimOrNull(String value) {
        return value != null ? value.trim() : null;
    }

    public MedicalServiceResponse toResponse(MedicalService service) {
        Specialty specialty = service.getSpecialty();
        return MedicalServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .price(service.getPrice())
                .estimatedDurationMin(service.getEstimatedDurationMin())
                .description(service.getDescription())
                .isActive(service.getIsActive())
                .specialty(specialty != null ? MedicalServiceResponse.SpecialtySummary.builder()
                        .id(specialty.getId())
                        .name(specialty.getName())
                        .slug(specialty.getSlug())
                        .build() : null)
                .build();
    }
}
