package com.fallguys.subscription.api.shared;

/**
 * subscription 도메인이 결제(payment) 모듈에 구독 결제를 요청하기 위한 포트 인터페이스
 *
 * <p>payment 모듈을 직접 import하지 않도록 분리된 Anti-Corruption Layer
 * 구현체는 app-main(또는 payment 모듈 내)에서 제공
 */
public interface  ExternalPaymentPort {

    /**
     * 구독 플랜 변경에 필요한 결제를 요청합니다.
     *
     * @param employerId 결제 주체인 고용주 ID
     * @param planType   변경할 플랜 타입 (PRO, PRIME)
     * @param amount     결제 금액 (KRW, 양수)
     * @param billingKey PortOne에서 발급받은 빌링키
     * @return 결제 처리 결과
     */
    PaymentResult requestSubscriptionPayment(Long employerId, String planType, long amount, String billingKey);

    PaymentResult verifyOneTimeSubscriptionPayment(Long employerId, String planType, long amount, String paymentId);

    /**
     * 특정 고용주의 다음 정기결제 예정일을 조회합니다.
     * payment 모듈 내의 BillingKey 정보를 바탕으로 조회합니다.
     *
     * @param employerId 조회할 고용주의 userId
     * @return 다음 결제 예정 시간 (결제 정보가 없거나 무료 플랜이면 null 반환 가능)
     */
    java.time.LocalDateTime getNextBillingDate(Long employerId);

    /**
     * 결제 결과를 담는 중간 DTO. payment 모듈 SubscriptionPaymentResult를 직접 노출하지 않기 위해 사용합니다.
     *
     * @param success      결제 성공 여부
     * @param billingId    생성된 결제 레코드 ID
     * @param errorCode    실패 시 에러 코드 (성공 시 null)
     * @param errorMessage 실패 시 에러 메시지 (성공 시 null)
     */
    record PaymentResult(
            boolean success,
            Long billingId,
            String errorCode,
            String errorMessage
    ) {}
}
