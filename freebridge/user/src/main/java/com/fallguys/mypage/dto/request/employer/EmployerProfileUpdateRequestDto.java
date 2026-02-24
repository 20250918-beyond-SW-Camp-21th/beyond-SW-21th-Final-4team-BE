package com.fallguys.mypage.dto.request.employer;

public record EmployerProfileUpdateRequestDto(
        String companyName,
        String industry,
        String scale,
        String location,
        String websiteUrl,
        String description
) {}
