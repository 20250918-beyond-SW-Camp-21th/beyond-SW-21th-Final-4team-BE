package com.fallguys.common.api.payment;

/**
 * 구독 결제 처리 인터페이스 — 타 모듈(subscription 등)에서 멤버십 변경 시 결제를 요청할 때 사용
 *
 * <p>사용 예:
 * <pre>{@code
 * @RequiredArgsConstructor
 * public class SubscriptionService {
 *     private final SubscriptionPaymentQuery subscriptionPaymentQuery;
 *
 *     public void upgradePlan(Long employerId, String planType, long amount, String billingKey) {
 *         SubscriptionPaymentResult result =
 *             subscriptionPaymentQuery.processSubscriptionPayment(employerId, planType, amount, billingKey);
 *
 *         if (!result.success()) {
 *             // 플랜 변경 롤백 처리
 *             throw new BusinessException(ErrorCode.PAYMENT_FAILED);
 *         }
 *         // 플랜 변경 확정 처리
 *     }
 * }
 * }</pre>
 */
public interface SubscriptionPaymentQuery {

    /**
     * 빌링키를 이용해 구독 결제를 처리합니다.
     *
     * <p>처리 내용:
     * <ul>
     *   <li>PortOne V2 API로 빌링키 즉시 결제 요청</li>
     *   <li>기존 활성 빌링키 비활성화 후 새 빌링키 저장</li>
     *   <li>SubscriptionBilling 레코드 생성 (PAID 또는 FAILED)</li>
     *   <li>PLATFORM_REVENUE 지갑 크레딧 (성공 시)</li>
     * </ul>
     *
     * @param employerId 고용주 ID (결제 주체)
     * @param planType   구독 플랜 ("PRO" 또는 "PRIME")
     * @param amount     결제 금액 (KRW, 양수여야 함)
     * @param billingKey PortOne에서 발급받은 빌링키
     * @return 결제 결과 — success 필드로 성공 여부 판단 후 처리
     */
    SubscriptionPaymentResult processSubscriptionPayment(
            Long employerId, String planType, long amount, String billingKey);

    SubscriptionPaymentResult verifyOneTimeSubscriptionPayment(
            Long employerId, String planType, long amount, String paymentId);

    /**
     * 구독 업그레이드 시 결제를 처리하고, 결제 결과와 다음 결제일을 한 번에 반환합니다.
     *
     * <p>처리 내용:
     * <ul>
     *   <li>PortOne V2 API로 빌링키 즉시 결제 요청 (1회)</li>
     *   <li>기존 활성 빌링키 비활성화 후 새 빌링키 저장</li>
     *   <li>SubscriptionBilling 레코드 생성 (PAID 또는 FAILED)</li>
     *   <li>PLATFORM_REVENUE 지갑 크레딧 (성공 시)</li>
     *   <li>BillingKey 엔티티의 nextBillingDate 반환 (성공 시)</li>
     * </ul>
     *
     * @param employerId 고용주 ID (결제 주체)
     * @param planType   구독 플랜 ("PRO" 또는 "PRIME")
     * @param amount     결제 금액 (KRW, 양수여야 함)
     * @param billingKey PortOne에서 발급받은 빌링키
     * @return 결제 결과 + 다음 결제일 — success 필드로 성공 여부 판단 후 처리
     */
    SubscriptionUpgradeResult processSubscriptionUpgrade(
            Long employerId, String planType, long amount, String billingKey);

    /**
     * 현재 활성 billingKey 기준 다음 정기결제 예정일을 조회합니다.
     *
     * @param employerId 고용주 ID
     * @return 다음 결제 예정일시, 없으면 null
     */
    java.time.LocalDateTime getNextBillingDate(Long employerId);
}
