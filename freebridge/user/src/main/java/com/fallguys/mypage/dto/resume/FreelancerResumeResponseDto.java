package com.fallguys.mypage.dto.resume;

import java.util.List;

public record FreelancerResumeResponseDto(
        List<EducationDto> educations,
        List<CareerDto> careers
        // 자격증(Certifications) 등 필요 시 추가
) {}
