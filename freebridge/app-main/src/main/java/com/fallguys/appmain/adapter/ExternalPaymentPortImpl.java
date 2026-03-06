package com.fallguys.appmain.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * user 모듈(subscription)에서 정의한 ExternalPaymentPort를 구현하고,
 * payment 모듈의 SubscriptionPaymentQuery를 호출하는 어댑터입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalPaymentPortImpl {

    // private final SubscriptionPaymentQuery subscriptionPaymentQuery; // TODO: payment 모듈 구현 완료 시 주입
    private final Environment environment;


    public PaymentResult requestSubscriptionPayment(Long employerId, String planType, long amount, String billingKey) {
        if (!isStubEnabled()) {
            log.error("[ExternalPaymentPortImpl] Stub payment disabled in this environment.");
            return new PaymentResult(false, null, "STUB_DISABLED", "Stub payment disabled in this environment");
        }
        log.warn("[ExternalPaymentPortImpl] 실제 결제 모듈(SubscriptionPaymentQuery) 미주입 상태입니다. Stub 결제로 처리합니다.");
        log.info("[ExternalPaymentPortImpl] 결제 Stub 요청 (employerId: {}, planType: {}, amount: {})",
                employerId, planType, amount);

        /* TODO: payment 모듈 구현 완료 및 빈 등록 확인 시 아래 코드로 복구
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

        // payment 모듈 미구현 상태에서 성공 Stub 반환
        return new PaymentResult(
                true,
                Math.abs(java.util.UUID.randomUUID().getMostSignificantBits()), // 임시 billingId
                null,
                null
        );
    }


    public LocalDateTime getNextBillingDate(Long employerId) {
        log.warn("[ExternalPaymentPortImpl] 실제 결제 모듈 미주입 상태입니다. Stub 결제일을 반환합니다.");

        /* TODO: payment 모듈 구현 완료 후 아래 코드 사용
        return subscriptionPaymentQuery.getNextBillingDate(employerId);
        */

        // 결제 모듈 구현 전 임시로 다음달 1일 09시 반환
        return LocalDateTime.now().plusMonths(1).withDayOfMonth(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
    }

    public record PaymentResult(boolean success, Long billingId, String errorCode, String errorMessage) {}

    private boolean isStubEnabled() {
        boolean profileEnabled = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equalsIgnoreCase(profile) || "test".equalsIgnoreCase(profile));
        boolean propertyEnabled = Boolean.parseBoolean(environment.getProperty("payment.stub.enabled", "false"));
        return profileEnabled || propertyEnabled;
    }
}
