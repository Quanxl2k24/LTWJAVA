package org.example.feature.doctorprofile.repository;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.example.feature.auth.model.User;
import org.example.feature.doctorprofile.dto.DoctorProfileFilterRequest;
import org.example.feature.doctorprofile.model.DoctorProfile;
import org.example.feature.specialty.model.Specialty;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class DoctorProfileSpecification {

    private DoctorProfileSpecification() {
    }

    public static Specification<DoctorProfile> filterBy(DoctorProfileFilterRequest filter, boolean publicOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<DoctorProfile, User> userJoin = root.join("user", JoinType.INNER);
            Join<DoctorProfile, Specialty> specialtyJoin = root.join("specialty", JoinType.INNER);

            if (filter != null) {
                // Từ khóa tìm kiếm theo họ tên bác sĩ hoặc học hàm/học vị (title)
                if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                    String pattern = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                    Predicate byName = cb.like(cb.lower(userJoin.get("fullName")), pattern);
                    Predicate byTitle = cb.like(cb.lower(root.get("title")), pattern);
                    predicates.add(cb.or(byName, byTitle));
                }

                // Lọc theo chuyên khoa
                if (filter.getSpecialtyId() != null) {
                    predicates.add(cb.equal(specialtyJoin.get("id"), filter.getSpecialtyId()));
                }

                // Lọc theo khoảng giá khám (minFee - maxFee)
                if (filter.getMinFee() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("consultationFee"), filter.getMinFee()));
                }
                if (filter.getMaxFee() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("consultationFee"), filter.getMaxFee()));
                }

                // Lọc theo số năm kinh nghiệm tối thiểu
                if (filter.getMinExperience() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("experienceYears"), filter.getMinExperience()));
                }

                // Lọc theo trạng thái sẵn sàng (nếu không bị ép publicOnly)
                if (!publicOnly && filter.getIsAvailable() != null) {
                    predicates.add(cb.equal(root.get("isAvailable"), filter.getIsAvailable()));
                }
            }

            // Chế độ công khai: Bắt buộc hồ sơ sẵn sàng + tài khoản bác sĩ đang hoạt động + chuyên khoa đang mở
            if (publicOnly) {
                predicates.add(cb.isTrue(root.get("isAvailable")));
                predicates.add(cb.isTrue(userJoin.get("isActive")));
                predicates.add(cb.isTrue(specialtyJoin.get("isActive")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
