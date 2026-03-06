package com.fallguys.subscription.api.response;

import java.time.LocalDateTime;

/**
 * 구독 정보 조회 응답 DTO
 */
public record SubscriptionResponse(
        String planGrade,          // BASIC, PRO, PRIME
        double feeRate,            // 수수료율 (예: 10.0)
        int monthlyPrice,          // 월 구독료 (예: 19900)
        String status,             // ACTIVE, CANCEL_RESERVED, EXPIRED
        LocalDateTime nextBillingDate  // 다음 결제일 (취소 시 만료일)
) {}
