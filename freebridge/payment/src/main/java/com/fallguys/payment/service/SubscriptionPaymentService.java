package com.fallguys.payment.service;

import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.entity.BillingKey;

public interface SubscriptionPaymentService {

    /**
     * 최초 구독 결제 처리 (프론트엔드 → 백엔드 호출)
     * - 프론트에서 PortOne SDK로 빌링키를 발급받은 후 전달
     * - PortOne 테스트 모드: 테스트 카드(4111 1111 1111 1111)로 실제 돈 없이 처리됨
     * - PortOne에 즉시 결제 요청 후 BillingKey 엔티티 저장
     */
    SubscriptionPaymentResponse processPayment(SubscriptionPaymentRequest request);

    /**
     * 스케줄러 자동결제 전용 (매월 1일 자동 실행)
     * - 저장된 BillingKey를 사용해 PortOne에 결제 요청만 함
     * - BillingKey 엔티티는 변경하지 않음 (새로 저장하거나 비활성화하지 않음)
     */
    void chargeScheduled(BillingKey billingKey, long amount);

    SubscriptionBillingItem getBillingById(Long billingId);
}
