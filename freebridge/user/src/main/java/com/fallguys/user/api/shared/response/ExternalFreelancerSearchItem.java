package com.fallguys.user.api.shared.response;

import java.util.List;

public record ExternalFreelancerSearchItem(
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
