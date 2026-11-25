package org.example.feature.medicalservice.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.example.feature.medicalservice.dto.MedicalServiceFilterRequest;
import org.example.feature.medicalservice.model.MedicalService;
import org.example.feature.specialty.model.Specialty;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class MedicalServiceSpecification {

    private MedicalServiceSpecification() {
    }

    public static Specification<MedicalService> filterBy(MedicalServiceFilterRequest filter, boolean publicOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Bắt buộc dùng LEFT JOIN để không làm rớt các dịch vụ tổng quát (specialty_id IS NULL)
            Join<MedicalService, Specialty> specialtyJoin = root.join("specialty", JoinType.LEFT);

            if (filter != null) {
                if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                    String pattern = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(root.get("name")), pattern));
                }

                if (filter.getSpecialtyId() != null) {
                    predicates.add(cb.equal(specialtyJoin.get("id"), filter.getSpecialtyId()));
                }

                if (Boolean.TRUE.equals(filter.getGeneralOnly())) {
                    predicates.add(cb.isNull(root.get("specialty")));
                }

                if (filter.getMinPrice() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
                }

                if (filter.getMaxPrice() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
                }

                if (filter.getMaxDuration() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("estimatedDurationMin"), filter.getMaxDuration()));
                }

                if (!publicOnly && filter.getIsActive() != null) {
                    predicates.add(cb.equal(root.get("isActive"), filter.getIsActive()));
                }
            }

            // Điều kiện hiển thị công khai: dịch vụ đang bật VÀ (là dịch vụ chung HOẶC chuyên khoa đang bật)
            if (publicOnly) {
                predicates.add(cb.isTrue(root.get("isActive")));
                Predicate noSpecialty = cb.isNull(root.get("specialty"));
                Predicate activeSpecialty = cb.isTrue(specialtyJoin.get("isActive"));
                predicates.add(cb.or(noSpecialty, activeSpecialty));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
