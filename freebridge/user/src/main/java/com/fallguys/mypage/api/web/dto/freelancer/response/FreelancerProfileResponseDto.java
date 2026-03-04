package com.fallguys.mypage.api.web.dto.freelancer.response;

public record FreelancerProfileResponseDto(
        FreelancerBasicProfileDto basicProfile,
        FreelancerStatsDto stats
) {}
