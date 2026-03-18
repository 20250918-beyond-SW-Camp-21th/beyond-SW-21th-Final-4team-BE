package com.fallguys.mypage.api.web.dto.employer.response;

public record EmployerProfilePreviewResponseDto(
        Long employerId,
        Long userId,
        String companyName,
        String industry,
        String scale,
        String location,
        String websiteUrl,
        String phone,
        String description,
        String logoUrl
) {}
