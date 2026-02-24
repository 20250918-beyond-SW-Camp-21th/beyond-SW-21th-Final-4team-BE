package com.fallguys.mypage.dto.employer.response;

public record EmployerBasicProfileDto(
        String companyName,
        String industry,
        String scale, // Enum name
        String location,
        String websiteUrl,
        String description,
        String logoUrl,
        String status // Enum name
) {}
