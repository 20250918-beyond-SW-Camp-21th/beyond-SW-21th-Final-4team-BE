package com.fallguys.appmain.adapter;

import com.fallguys.common.api.payment.SubscriptionPaymentQuery;
import com.fallguys.common.api.payment.SubscriptionPaymentResult;
import com.fallguys.subscription.api.shared.ExternalPaymentPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * user 모듈(subscription)에서 정의한 ExternalPaymentPort를 구현하고,
 * payment 모듈의 SubscriptionPaymentQuery를 호출하는 어댑터입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalPaymentPortImpl implements ExternalPaymentPort {

    private final SubscriptionPaymentQuery subscriptionPaymentQuery;

    @Override
    public PaymentResult requestSubscriptionPayment(Long employerId, String planType, long amount, String billingKey) {
        SubscriptionPaymentResult result = subscriptionPaymentQuery.processSubscriptionPayment(
                employerId, planType, amount, billingKey
        );

        if (result == null) {
            log.error("[ExternalPaymentPortImpl] 결제 모듈 응답이 null 입니다. (employerId: {})", employerId);
            return new PaymentResult(false, null, "PAYMENT_RESULT_NULL", "결제 모듈 응답이 비어 있습니다.");
        }
        return new PaymentResult(
                result.success(),
                result.billingId(),
                result.errorCode(),
                result.errorMessage()
        );
    }

    @Override
    public PaymentResult verifyOneTimeSubscriptionPayment(Long employerId, String planType, long amount, String paymentId) {
        SubscriptionPaymentResult result = subscriptionPaymentQuery.verifyOneTimeSubscriptionPayment(
                employerId, planType, amount, paymentId
        );

        if (result == null) {
            log.error("[ExternalPaymentPortImpl] one-time subscription verify result is null. (employerId: {})", employerId);
            return new PaymentResult(false, null, "PAYMENT_RESULT_NULL", "결제 모듈 응답이 비어 있습니다.");
        }

        return new PaymentResult(
                result.success(),
                result.billingId(),
                result.errorCode(),
                result.errorMessage()
        );
    }

    @Override
    public LocalDateTime getNextBillingDate(Long employerId) {
        return subscriptionPaymentQuery.getNextBillingDate(employerId);
    }
}
