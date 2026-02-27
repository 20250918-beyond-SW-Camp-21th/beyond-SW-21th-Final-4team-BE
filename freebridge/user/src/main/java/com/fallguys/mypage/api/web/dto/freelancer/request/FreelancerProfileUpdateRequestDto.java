package com.fallguys.mypage.api.web.dto.freelancer.request;

import java.util.List;

public record FreelancerProfileUpdateRequestDto(
        String job,
        String introduction,
        Integer careerYears,
        Long wage,
        List<String> skills
) {}
