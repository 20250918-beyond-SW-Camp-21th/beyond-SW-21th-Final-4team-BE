package com.fallguys.mypage.api.web.dto.freelancer.response;

import java.time.LocalDate;

public record FreelancerPreviewCertificationDto(
        String name,
        String issuer,
        LocalDate acquisitionDate
) {}
