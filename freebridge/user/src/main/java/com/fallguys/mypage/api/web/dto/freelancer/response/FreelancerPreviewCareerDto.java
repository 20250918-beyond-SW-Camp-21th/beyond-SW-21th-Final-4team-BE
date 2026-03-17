package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.time.LocalDate;

public record FreelancerPreviewCareerDto(
        String companyName,
        String department,
        String position,
        String jobType,
        String employmentType,
        LocalDate startDate,
        LocalDate endDate,
        String description
) {}
