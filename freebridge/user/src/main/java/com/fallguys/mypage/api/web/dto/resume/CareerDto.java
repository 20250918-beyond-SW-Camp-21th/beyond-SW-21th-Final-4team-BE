package com.fallguys.mypage.api.web.dto.resume;

public record CareerDto(
        Long careerId,
        String companyName,
        String department,
        String role,
        String jobType,
        String employmentType,
        String startDate,
        String endDate,
        String description
) {}
