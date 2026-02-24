package com.fallguys.mypage.dto.resume;

public record CareerDto(
        Long careerId,
        String companyName,
        String role,
        String startDate,
        String endDate,
        String description
) {}
