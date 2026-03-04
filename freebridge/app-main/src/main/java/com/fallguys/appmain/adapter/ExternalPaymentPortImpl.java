package com.fallguys.appmain.adapter;

import com.fallguys.payment.api.shared.SubscriptionPaymentQuery;
import com.fallguys.payment.api.shared.SubscriptionPaymentResult;
import com.fallguys.subscription.api.shared.ExternalPaymentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * user 모듈(subscription 도메인)의 ExternalPaymentPort 인터페이스를 구현하여,
 * payment 모듈의 SubscriptionPaymentQuery를 호출하는 어댑터 클래스입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalPaymentPortImpl implements ExternalPaymentPort {

    private final SubscriptionPaymentQuery subscriptionPaymentQuery;

    @Override
    public PaymentResult requestSubscriptionPayment(Long employerId, String planType, long amount, String billingKey) {
        log.info("[ExternalPaymentPortImpl] 결제 모듈로 정기결제 요청 전달 (employerId: {}, planType: {}, amount: {})",
                employerId, planType, amount);

        // payment 모듈의 공유 API 호출
        SubscriptionPaymentResult result = subscriptionPaymentQuery.processSubscriptionPayment(
                employerId, planType, amount, billingKey
        );

        log.info("[ExternalPaymentPortImpl] 결제 처리 결과 수신 (success: {}, errorCode: {})",
                result.success(), result.errorCode());

        // payment 모듈의 결과를 subscription 도메인의 DTO로 매핑하여 반환
        return new PaymentResult(
                result.success(),
                result.billingId(),
                result.errorCode(),
                result.errorMessage()
        );
    }
}
