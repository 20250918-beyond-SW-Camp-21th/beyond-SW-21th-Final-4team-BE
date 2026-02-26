package com.fallguys.mypage.dto.employer.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmployerProfileResponseDto(
        EmployerBasicProfileDto basicProfile,
        EmployerRatingDto ratings,
        EmployerProjectStatusDto projectStatus,
        
        @Schema(description = "고용주 CRM 마케팅 알림 플래그")
        CrmAlertsResponseDto crmAlerts
) {}
