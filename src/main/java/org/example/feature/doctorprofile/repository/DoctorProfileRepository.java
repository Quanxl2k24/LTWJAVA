package org.example.feature.doctorprofile.repository;

import org.example.feature.doctorprofile.model.DoctorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long>, JpaSpecificationExecutor<DoctorProfile> {

    boolean existsByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "specialty"})
    Optional<DoctorProfile> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"user", "specialty"})
    Optional<DoctorProfile> findWithDetailsByUserId(Long userId);

    @Override
    @EntityGraph(attributePaths = {"user", "specialty"})
    Page<DoctorProfile> findAll(Specification<DoctorProfile> spec, Pageable pageable);
}
