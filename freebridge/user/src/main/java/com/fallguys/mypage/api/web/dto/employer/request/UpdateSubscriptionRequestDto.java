package com.fallguys.mypage.api.web.dto.employer.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// 구독정보 변경 요청 Dto
public record UpdateSubscriptionRequestDto(
        @NotBlank(message = "변경할 플랜을 입력해주세요.")
        @Pattern(regexp = "^(?i)(BASIC|PRO|PRIME)$", message = "유효하지 않은 플랜 값입니다. (BASIC, PRO, PRIME)")
        String targetPlan,

        String billingKey,

        String paymentId
) {}
