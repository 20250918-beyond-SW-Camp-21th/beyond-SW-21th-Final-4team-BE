package com.fallguys.subscription.service;

import com.fallguys.subscription.api.request.SubscriptionCancelRequest;
import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionResponse;
import com.fallguys.subscription.api.shared.ExternalPaymentPort;
import com.fallguys.subscription.api.shared.ExternalSubscriptionPort;
import com.fallguys.subscription.entity.PlanGrade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구독 도메인 핵심 비즈니스 로직 구현체.
 *
 * <h3>플랜 변경 정책</h3>
 * <ul>
 *   <li><b>업그레이드</b> (BASIC→PRO, BASIC→PRIME, PRO→PRIME): 즉시 결제 후 즉시 플랜 변경</li>
 *   <li><b>다운그레이드 (유료→유료)</b> (PRIME→PRO): 당월 말까지 현재 플랜 유지, 다음 결제일부터 하위 플랜 전환.
 *       다운그레이드 월에는 추가 결제 없음.</li>
 *   <li><b>BASIC 전환</b> (PRO/PRIME→BASIC): {@link #cancelSubscription} 를 통해 처리</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final ExternalSubscriptionPort externalSubscriptionPort;
    private final ExternalPaymentPort externalPaymentPort;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
        log.info("[SubscriptionService] 구독 정보 조회 요청 (userId: {})", userId);

        PlanGrade planGrade = externalSubscriptionPort.getCurrentPlan(userId);

        return new SubscriptionResponse(
                planGrade.name(),
                planGrade.getFeeRate(),
                planGrade.getMonthlyPrice(),
                "ACTIVE",   // TODO: 실제 status 조회 필요 시 ExternalSubscriptionPort에 메서드 추가
                null        // TODO: nextBillingDate 별도 저장 테이블 구현 시 채울 것
        );
    }

    /**
     * 구독 플랜 변경.
     *   업그레이드: 결제 성공 시 즉시 변경
     *   다운그레이드(Prime→Pro): 결제 없이 다음 결제일로 변경 예약 (당월 현 플랜 유지)
     *   BASIC으로 변경 시: 취소 처리를 위해 cancelSubscription 사용 권장이나, 직접 BASIC 요청 시도 처리
     */
    @Override
    @Transactional
    public void changePlan(Long userId, SubscriptionChangeRequest request) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
        if (request == null || request.targetPlanGrade() == null || request.targetPlanGrade().isBlank()) {
            throw new IllegalArgumentException("변경할 플랜 값이 필요합니다.");
        }

        PlanGrade targetGrade;
        try {
            targetGrade = PlanGrade.valueOf(request.targetPlanGrade().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("유효하지 않은 플랜 값입니다: " + request.targetPlanGrade());
        }

        PlanGrade currentGrade = externalSubscriptionPort.getCurrentPlan(userId);
        if (currentGrade == targetGrade) {
            throw new IllegalArgumentException("현재와 동일한 플랜으로는 변경할 수 없습니다.");
        }

        // BASIC으로 변경 = 구독 취소 처리
        if (targetGrade == PlanGrade.BASIC) {
            log.info("[SubscriptionService] BASIC 전환 요청 → 구독 취소 처리 (userId: {})", userId);
            externalSubscriptionPort.changePlan(userId, PlanGrade.BASIC);
            return;
        }

        boolean isUpgrade = targetGrade.ordinal() > currentGrade.ordinal();

        if (isUpgrade) {
            // ── 업그레이드: 결제 먼저, 성공 시 즉시 플랜 변경 ──
            if (request.billingKey() == null || request.billingKey().isBlank()) {
                throw new IllegalArgumentException("유료 플랜 변경 시 billingKey가 필요합니다.");
            }

            log.info("[SubscriptionService] 업그레이드 결제 트리거 (userId: {}, {} -> {})", userId, currentGrade, targetGrade);
            ExternalPaymentPort.PaymentResult result = externalPaymentPort.requestSubscriptionPayment(
                    userId,
                    targetGrade.name(),
                    targetGrade.getMonthlyPrice(),
                    request.billingKey()
            );

            if (!result.success()) {
                log.warn("[SubscriptionService] 업그레이드 결제 실패 (userId: {}, errorCode: {}, message: {})",
                        userId, result.errorCode(), result.errorMessage());
                throw new IllegalStateException("구독 결제가 실패하였습니다. 사유: " + result.errorMessage());
            }

            log.info("[SubscriptionService] 업그레이드 완료 (userId: {}, billingId: {}, plan: {})",
                    userId, result.billingId(), targetGrade);
            externalSubscriptionPort.changePlan(userId, targetGrade);

        } else {
            // ── 다운그레이드 (유료→유료, PRIME→PRO): 결제 없이 다음 결제일로 예약 ──
            // 당월 말까지 현재 플랜 유지, 다음 결제일부터 하위 플랜 적용
            log.info("[SubscriptionService] 다운그레이드 예약 (userId: {}, {} -> {}) — 다음 결제일 적용",
                    userId, currentGrade, targetGrade);
            externalSubscriptionPort.schedulePlanDowngrade(userId, targetGrade);
        }
    }

    @Override
    @Transactional
    public void cancelSubscription(Long userId, SubscriptionCancelRequest request) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }

        PlanGrade currentGrade = externalSubscriptionPort.getCurrentPlan(userId);
        if (currentGrade == PlanGrade.BASIC) {
            throw new IllegalStateException("이미 무료(BASIC) 플랜 사용 중이므로 취소할 수 없습니다.");
        }

        String cancelReason = (request != null) ? request.cancelReason() : null;
        log.info("[SubscriptionService] 구독 취소 요청 (userId: {}, reason: {})", userId, cancelReason);
        externalSubscriptionPort.cancelSubscription(userId, cancelReason);
    }
}
