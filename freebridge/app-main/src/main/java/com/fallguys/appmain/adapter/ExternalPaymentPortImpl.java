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
// @RequiredArgsConstructor // TODO: payment 모듈 연동 시 주석 해제 및 의존성 주입
public class ExternalPaymentPortImpl implements ExternalPaymentPort {

    // private final SubscriptionPaymentQuery subscriptionPaymentQuery; // TODO: payment 모듈 구현 완료 시 주석 해제

    @Override
    public PaymentResult requestSubscriptionPayment(Long employerId, String planType, long amount, String billingKey) {
        log.warn("[ExternalPaymentPortImpl] 실제 결제 모듈(SubscriptionPaymentQuery) 주입 대기 중입니다. Stub(가짜) 결제 성공으로 처리합니다.");
        log.info("[ExternalPaymentPortImpl] 결제 가짜(Stub) 요청 (employerId: {}, planType: {}, amount: {})",
                employerId, planType, amount);

        /* TODO: payment 모듈 구현 완료 및 빈 등록이 확인되면 아래 코드로 복구
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
        */

        // payment 모듈 없이 즉시 결제 성공 객체 리턴
        return new PaymentResult(
                true,
                Math.abs(java.util.UUID.randomUUID().getMostSignificantBits()), // 임의의 billingId
                null,
                null
        );
    }

    @Override
    public java.time.LocalDateTime getNextBillingDate(Long employerId) {
        log.warn("[ExternalPaymentPortImpl] 실제 결제 모듈 주입 대기 중입니다. Stub(가짜) 결제 예정일을 반환합니다.");
        
        /* TODO: payment 모듈 구현 완료 시 교체
        return subscriptionPaymentQuery.getNextBillingDate(employerId);
        */
        
        // 결제 모듈 구현 전까지 임시로 다음달 1일 09시 리턴
        return java.time.LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
    }
}
