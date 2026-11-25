package org.example.feature.patientProfiles;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.feature.patientProfiles.dto.PatientProfileResponse;
import org.example.feature.patientProfiles.model.PatientProfile;
import org.example.feature.patientProfiles.repository.PatientProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientProfilesService {

    private final PatientProfileRepository patientProfileRepository;

    @Transactional(readOnly = true)
    public List<PatientProfileResponse> getMyPatientProfiles(Long userId) {
        return patientProfileRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PatientProfileResponse mapToResponse(PatientProfile profile) {
        return PatientProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .identityCard(profile.getIdentityCard())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .address(profile.getAddress())
                .medicalHistory(profile.getMedicalHistory())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
