package com.fallguys.mypage.api.web.dto.employer.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고용주 CRM 알림 응답")
public record CrmAlertsResponseDto(
        @Schema(description = "첫 공고 등록 유도 여부", example = "true")
        boolean isFirstJobEncouraged,
        @Schema(description = "검토 대기 지원자 여부", example = "true")
        boolean hasPendingApplicants,
        @Schema(description = "계약 전환 유도 여부", example = "false")
        boolean isContractConversionNeeded,
        @Schema(description = "재채용 권장 여부", example = "false")
        boolean isRehiringRecommended,
        @Schema(description = "구독 상태 확인 필요 여부", example = "false")
        boolean isSubscriptionAttentionNeeded,
        @Schema(description = "PRO 업셀 대상 여부", example = "true")
        boolean isPremiumUpsellEligible,
        @Schema(description = "PRIME 업셀 대상 여부", example = "false")
        boolean isPrimeUpsellEligible,
        @Schema(description = "권장 업셀 타깃", example = "PRO")
        String upsellTarget
) {
}
