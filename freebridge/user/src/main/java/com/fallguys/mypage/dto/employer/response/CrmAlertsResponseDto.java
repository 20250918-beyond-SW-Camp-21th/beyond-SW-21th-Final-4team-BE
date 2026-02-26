package com.fallguys.mypage.dto.employer.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고용주 CRM 마케팅 알림 플래그")
public record CrmAlertsResponseDto(
        @Schema(description = "프리미엄 요금제 업셀링 대상 여부", example = "true")
        boolean isPremiumUpsellEligible
) {
}
