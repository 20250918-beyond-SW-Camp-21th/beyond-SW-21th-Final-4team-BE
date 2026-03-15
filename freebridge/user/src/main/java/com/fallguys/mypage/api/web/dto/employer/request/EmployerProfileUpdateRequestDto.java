package com.fallguys.mypage.api.web.dto.employer.request;

public record EmployerProfileUpdateRequestDto(
        String companyName,
        String industry,
        String scale,
        String location,
        String websiteUrl,
        String description,
        String phone
) {}
