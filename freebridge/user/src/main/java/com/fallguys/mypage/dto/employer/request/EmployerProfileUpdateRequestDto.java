package com.fallguys.mypage.dto.employer.request;

public record EmployerProfileUpdateRequestDto(
        String companyName,
        String industry,
        String scale,
        String location,
        String websiteUrl,
        String description
) {}
