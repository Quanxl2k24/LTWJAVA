package org.example.feature.doctorprofile.dto;

import org.example.feature.auth.model.User;
import org.example.feature.doctorprofile.model.DoctorProfile;
import org.example.feature.specialty.model.Specialty;
import org.springframework.stereotype.Component;

@Component
public class DoctorProfileMapper {

    public String normalizeTitle(String title) {
        return title != null ? title.trim() : null;
    }

    public DoctorProfilePublicResponse toPublicResponse(DoctorProfile profile) {
        User user = profile.getUser();
        Specialty specialty = profile.getSpecialty();

        return DoctorProfilePublicResponse.builder()
                .id(profile.getId())
                .title(profile.getTitle())
                .experienceYears(profile.getExperienceYears())
                .biography(profile.getBiography())
                .consultationFee(profile.getConsultationFee())
                .isAvailable(profile.getIsAvailable())
                .user(user != null ? DoctorProfilePublicResponse.UserPublicSummary.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .gender(user.getGender())
                        .build() : null)
                .specialty(specialty != null ? DoctorProfilePublicResponse.SpecialtySummary.builder()
                        .id(specialty.getId())
                        .name(specialty.getName())
                        .slug(specialty.getSlug())
                        .build() : null)
                .build();
    }

    public DoctorProfileAdminResponse toAdminResponse(DoctorProfile profile) {
        User user = profile.getUser();
        Specialty specialty = profile.getSpecialty();

        return DoctorProfileAdminResponse.builder()
                .id(profile.getId())
                .title(profile.getTitle())
                .experienceYears(profile.getExperienceYears())
                .biography(profile.getBiography())
                .consultationFee(profile.getConsultationFee())
                .isAvailable(profile.getIsAvailable())
                .user(user != null ? DoctorProfileAdminResponse.UserAdminSummary.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .gender(user.getGender())
                        .phone(user.getPhone())
                        .email(user.getEmail())
                        .isActive(user.getIsActive())
                        .build() : null)
                .specialty(specialty != null ? DoctorProfileAdminResponse.SpecialtyAdminSummary.builder()
                        .id(specialty.getId())
                        .name(specialty.getName())
                        .slug(specialty.getSlug())
                        .isActive(specialty.getIsActive())
                        .build() : null)
                .build();
    }
}
