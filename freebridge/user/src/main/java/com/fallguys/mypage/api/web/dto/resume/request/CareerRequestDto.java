package com.fallguys.mypage.api.web.dto.resume.request;

import java.time.LocalDate;

public record CareerRequestDto(
        String companyName,
        String department,
        String position,
        String jobType,          // 직무 유형
        String employmentType,   // 고용 유형 (정규직, 계약직 등)
        LocalDate startDate,
        LocalDate endDate,       // null 이면 현재 재직중
        String description
) {}
