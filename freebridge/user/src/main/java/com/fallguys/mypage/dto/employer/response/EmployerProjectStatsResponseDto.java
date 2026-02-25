package com.fallguys.mypage.dto.employer.response;

public record EmployerProjectStatsResponseDto(
        Integer totalProjects,
        Integer activeApplicants,
        Integer contractedFreelancers
) {}
