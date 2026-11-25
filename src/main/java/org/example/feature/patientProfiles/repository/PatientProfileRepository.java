package org.example.feature.patientProfiles.repository;

import org.example.feature.patientProfiles.model.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    List<PatientProfile> findByUserIdOrderByCreatedAtDesc(Long userId);
}
