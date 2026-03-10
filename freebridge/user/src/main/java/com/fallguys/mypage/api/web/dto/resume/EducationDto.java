package com.fallguys.mypage.api.web.dto.resume;

public record EducationDto(
        Long educationId,
        String schoolType,
        String schoolName,
        String major,
        String admissionDate,
        String graduationDate,
        String status // 재학, 졸업, 휴학 등
) {}
