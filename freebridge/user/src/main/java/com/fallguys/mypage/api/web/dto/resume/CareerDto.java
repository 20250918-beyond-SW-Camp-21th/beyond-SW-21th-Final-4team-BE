package com.fallguys.mypage.api.web.dto.resume;

public record CareerDto(
        Long careerId,
        String companyName,
        String role,
        String startDate,
        String endDate,
        String description
) {}
