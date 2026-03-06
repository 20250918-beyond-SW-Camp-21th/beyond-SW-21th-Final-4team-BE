package com.fallguys.subscription.api.response;

import java.time.LocalDateTime;

/**
 * 구독 변경/취소 요청 처리 결과 응답 DTO.
 */
public record SubscriptionChangeResultResponse(
        String currentPlanGrade,
        String pendingPlanGrade,
        String status,
        LocalDateTime nextBillingDate,
        String message
) {
}
