package com.fallguys.common.api.payment;

/**
 * 구독 결제 처리 결과 — 타 모듈이 SubscriptionPaymentQuery를 통해 받는 응답 객체
 *
 * @param success      결제 성공 여부
 * @param billingId    생성된 SubscriptionBilling 레코드 ID
 * @param planType     결제된 플랜 (PRO / PRIME)
 * @param amount       결제 금액 (KRW)
 * @param status       SubscriptionBillingStatus 값 (PAID / FAILED)
 * @param errorCode    실패 시 에러 코드 (성공 시 null)
 * @param errorMessage 실패 시 에러 메시지 (성공 시 null)
 */
public record SubscriptionPaymentResult(
        boolean success,
        Long billingId,
        String planType,
        Long amount,
        String status,
        String errorCode,
        String errorMessage
) {}