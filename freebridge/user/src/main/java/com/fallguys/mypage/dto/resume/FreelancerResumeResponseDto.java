package com.fallguys.mypage.dto.resume;

import java.util.List;

public record FreelancerResumeResponseDto(
        List<EducationDto> educations,
        List<CareerDto> careers,
        List<CertificationDto> certifications
) {}
