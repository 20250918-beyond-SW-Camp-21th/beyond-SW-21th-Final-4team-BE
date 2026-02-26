package com.fallguys.mypage.dto.freelancer.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record FreelancerProfileResponseDto(
        FreelancerBasicProfileDto basicProfile,
        FreelancerStatsDto stats,
        FreelancerCrmAlertsDto crmAlerts    //
) {

    public record FreelancerCrmAlertsDto(
            @Schema(description = "희망 단가 인상 제안 대상 노출 여부 (최근 우수 평가자)", example = "true")
            boolean suggestRateIncrease
    ) {}
}
