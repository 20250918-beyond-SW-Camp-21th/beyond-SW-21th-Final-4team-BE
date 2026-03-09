package com.fallguys.mypage.api.web.dto.employer.response;

import java.util.List;

public record EmployerFreelancerSearchItemDto(
        Long freelancerId,
        Long userId,
        String name,
        String job,
        Integer careerYears,
        Long wage,
        String introduction,
        String avatarUrl,
        List<String> skills,
        String grade
) {}
