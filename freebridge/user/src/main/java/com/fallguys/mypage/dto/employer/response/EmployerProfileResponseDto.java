package com.fallguys.mypage.dto.employer.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고용주 마이페이지 프로필 응답")
public record EmployerProfileResponseDto(

        @Schema(description = "기본 프로필 정보")
        EmployerBasicProfileDto basicProfile,

        @Schema(description = "평점 정보")
        EmployerRatingDto ratings,

        @Schema(description = "프로젝트 진행 상태 정보")
        EmployerProjectStatusDto projectStatus,

        @Schema(description = "CRM 마케팅 알림 정보")
        CrmAlertsResponseDto crmAlerts

) {

    public EmployerProfileResponseDto {
        crmAlerts = crmAlerts == null
                ? CrmAlertsResponseDto.defaultValue()
                : crmAlerts;
    }
}