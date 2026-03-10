package com.fallguys.common.api.payment;

import java.time.LocalDateTime;

/**
 * 구독 업그레이드 결제 처리 결과 — 결제 결과와 다음 결제일을 함께 반환합니다.
 *
 * @param success         결제 성공 여부
 * @param billingId       생성된 SubscriptionBilling 레코드 ID
 * @param planType        결제된 플랜 (PRO / PRIME)
 * @param amount          결제 금액 (KRW)
 * @param status          SubscriptionBillingStatus 값 (PAID / FAILED)
 * @param errorCode       실패 시 에러 코드 (성공 시 null)
 * @param errorMessage    실패 시 에러 메시지 (성공 시 null)
 * @param nextBillingDate 다음 정기결제 예정일 (성공 시 설정, 실패 시 null)
 */
public record SubscriptionUpgradeResult(
        boolean success,
        Long billingId,
        String planType,
        Long amount,
        String status,
        String errorCode,
        String errorMessage,
        LocalDateTime nextBillingDate
) {}
