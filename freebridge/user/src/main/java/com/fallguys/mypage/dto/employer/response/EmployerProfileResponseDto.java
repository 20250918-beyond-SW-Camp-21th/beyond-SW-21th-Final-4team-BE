package com.fallguys.mypage.dto.employer.response;

public record EmployerProfileResponseDto(
        EmployerBasicProfileDto basicProfile,
        EmployerRatingDto ratings,
        EmployerProjectStatusDto projectStatus
) {}
