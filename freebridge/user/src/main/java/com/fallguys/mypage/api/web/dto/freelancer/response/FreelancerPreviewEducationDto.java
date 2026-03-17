package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.time.LocalDate;

public record FreelancerPreviewEducationDto(
        String schoolType,
        String schoolName,
        String major,
        String status,
        LocalDate entranceDate,
        LocalDate graduationDate
) {}
