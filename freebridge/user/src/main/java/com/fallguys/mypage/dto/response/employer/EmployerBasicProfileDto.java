package com.fallguys.mypage.dto.response.employer;

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
