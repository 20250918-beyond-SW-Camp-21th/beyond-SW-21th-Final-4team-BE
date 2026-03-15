package com.fallguys.mypage.api.web.dto.employer.response;

import com.fallguys.mypage.entity.employer.Employer;

public record EmployerBasicProfileDto(
        String companyName,
        String industry,
        String scale, // Enum name
        String location,
        String websiteUrl,
        String phone,
        String description,
        String logoUrl,
        String status // Enum name
) {
    public static EmployerBasicProfileDto from(Employer employer, String phone) {
        return new EmployerBasicProfileDto(
                employer.getCompanyName(),
                employer.getIndustry(),
                employer.getScale() != null ? employer.getScale().name() : null,
                employer.getLocation(),
                employer.getWebsiteUrl(),
                phone,
                employer.getDescription(),
                employer.getLogoUrl(),
                employer.getStatus() != null ? employer.getStatus().name() : null
        );
    }
}
