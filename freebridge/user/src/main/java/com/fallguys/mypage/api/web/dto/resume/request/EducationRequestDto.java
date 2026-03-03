package com.fallguys.mypage.api.web.dto.resume.request;

import java.time.LocalDate;

public record EducationRequestDto(
        String schoolType,   // 고등학교, 전문대, 4년제 대학교 등
        String schoolName,
        String major,
        String eduStatus,    // EduStatus enum name (GRADUATED, ATTENDING, LEAVE_OF_ABSENCE, OTHER)
        LocalDate entranceDate,
        LocalDate graduationDate
) {}
