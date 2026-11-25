package org.example.feature.medicalservice;

import org.example.common.api.PageResponse;
import org.example.common.security.UserPrincipal;
import org.example.feature.medicalservice.dto.*;
import org.springframework.data.domain.Pageable;

public interface MedicalServiceService {

    MedicalServiceResponse create(CreateMedicalServiceRequest request);

    PageResponse<MedicalServiceResponse> getAll(MedicalServiceFilterRequest filter, Pageable pageable, UserPrincipal currentUser);

    MedicalServiceResponse getById(Integer id, UserPrincipal currentUser);

    MedicalServiceResponse update(Integer id, UpdateMedicalServiceRequest request);

    MedicalServiceResponse updateStatus(Integer id, UpdateMedicalServiceStatusRequest request);
}
