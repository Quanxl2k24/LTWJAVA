package org.example.feature.specialty.repository;

import org.example.feature.specialty.model.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {

    Optional<Specialty> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlugAndIdNot(String slug, Integer id);

    boolean existsByNameAndIdNot(String name, Integer id);

    List<Specialty> findByIsActiveTrueOrderByNameAsc();

    @Query("SELECT s FROM Specialty s WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.slug) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:isActive IS NULL OR s.isActive = :isActive)")
    Page<Specialty> searchSpecialties(
            @Param("keyword") String keyword,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}

