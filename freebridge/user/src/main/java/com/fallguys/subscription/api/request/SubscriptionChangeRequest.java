package com.fallguys.subscription.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 구독 플랜 변경 요청 DTO
 */
public record SubscriptionChangeRequest(
        @NotBlank(message = "변경할 플랜을 입력해주세요.")
        @Pattern(regexp = "^(?i)(BASIC|PRO|PRIME)$", message = "유효하지 않은 플랜 값입니다. (BASIC, PRO, PRIME)")
        String targetPlanGrade,

        // PRO, PRIME 유료 플랜 변경 시 필수. BASIC 다운그레이드 시 null 허용.
        String billingKey,

        String paymentId
) {}

