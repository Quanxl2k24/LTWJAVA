package org.example.feature.medicalservice.repository;

import org.example.feature.medicalservice.model.MedicalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalServiceRepository extends JpaRepository<MedicalService, Integer>, JpaSpecificationExecutor<MedicalService> {

    @EntityGraph(attributePaths = {"specialty"})
    Optional<MedicalService> findWithSpecialtyById(Integer id);

    @Override
    @EntityGraph(attributePaths = {"specialty"})
    Page<MedicalService> findAll(Specification<MedicalService> spec, Pageable pageable);

    boolean existsByNameIgnoreCaseAndSpecialtyId(String name, Integer specialtyId);

    boolean existsByNameIgnoreCaseAndSpecialtyIsNull(String name);

    boolean existsByNameIgnoreCaseAndSpecialtyIdAndIdNot(String name, Integer specialtyId, Integer id);

    boolean existsByNameIgnoreCaseAndSpecialtyIsNullAndIdNot(String name, Integer id);
}
