package com.fallguys.mypage.dto.response.employer;

public record EmployerProfileResponseDto(
        EmployerBasicProfileDto basicProfile,
        EmployerRatingDto ratings,
        EmployerProjectStatusDto projectStatus
) {}
