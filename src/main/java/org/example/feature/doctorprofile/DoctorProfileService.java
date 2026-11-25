package org.example.feature.doctorprofile;

import org.example.common.api.PageResponse;
import org.example.common.security.UserPrincipal;
import org.example.feature.doctorprofile.dto.*;
import org.springframework.data.domain.Pageable;

public interface DoctorProfileService {

    DoctorProfileAdminResponse create(CreateDoctorProfileRequest request);

    PageResponse<?> getAll(DoctorProfileFilterRequest filter, Pageable pageable, UserPrincipal currentUser);

    Object getById(Long id, UserPrincipal currentUser);

    DoctorProfileAdminResponse getMyProfile(Long currentUserId);

    DoctorProfileAdminResponse update(Long id, UpdateDoctorProfileRequest request);

    DoctorProfileAdminResponse updateMyProfile(Long currentUserId, UpdateMyDoctorProfileRequest request);

    DoctorProfileAdminResponse updateAvailability(Long id, UpdateDoctorAvailabilityRequest request, UserPrincipal currentUser);
}
