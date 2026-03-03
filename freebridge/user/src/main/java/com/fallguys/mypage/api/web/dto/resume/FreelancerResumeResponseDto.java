package com.fallguys.mypage.api.web.dto.resume;

import java.util.List;

public record FreelancerResumeResponseDto(
        List<EducationDto> educations,
        List<CareerDto> careers,
        List<CertificationDto> certifications
) {}
