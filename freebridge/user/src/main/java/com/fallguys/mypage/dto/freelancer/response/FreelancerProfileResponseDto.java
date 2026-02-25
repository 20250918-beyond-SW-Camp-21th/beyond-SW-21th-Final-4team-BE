package com.fallguys.mypage.dto.freelancer.response;

public record FreelancerProfileResponseDto(
        FreelancerBasicProfileDto basicProfile,
        FreelancerStatsDto stats
) {}
