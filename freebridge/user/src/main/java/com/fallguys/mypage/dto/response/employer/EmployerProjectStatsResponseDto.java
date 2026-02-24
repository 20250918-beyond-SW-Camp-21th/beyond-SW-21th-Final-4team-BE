package com.fallguys.mypage.dto.response.employer;

public record EmployerProjectStatsResponseDto(
        Integer totalProjects,
        Integer activeApplicants,
        Integer contractedFreelancers
) {}
