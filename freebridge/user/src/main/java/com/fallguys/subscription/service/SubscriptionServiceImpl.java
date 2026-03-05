package com.fallguys.subscription.service;

import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionChangeResultResponse;
import com.fallguys.subscription.api.response.SubscriptionResponse;
import com.fallguys.subscription.api.shared.ExternalPaymentPort;
import com.fallguys.subscription.api.shared.ExternalSubscriptionPort;
import com.fallguys.subscription.entity.PlanGrade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final ExternalSubscriptionPort externalSubscriptionPort;
    private final ExternalPaymentPort externalPaymentPort;

    @Override
    @Transactional(readOnly = true)
    public SubscriptionResponse getSubscription(Long userId) {
        validateUserId(userId);

        PlanGrade currentGrade = externalSubscriptionPort.getCurrentPlan(userId);
        LocalDateTime nextBillingDate = null;
        if (currentGrade != PlanGrade.BASIC) {
            nextBillingDate = externalPaymentPort.getNextBillingDate(userId);
        }

        return new SubscriptionResponse(
                currentGrade.name(),
                currentGrade.getFeeRate(),
                currentGrade.getMonthlyPrice(),
                "ACTIVE",
                nextBillingDate
        );
    }

    @Override
    @Transactional
    public SubscriptionChangeResultResponse changePlan(Long userId, SubscriptionChangeRequest request) {
        validateUserId(userId);
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

        // BASIC 전환은 취소 API에서 nextBillingDate 예약 정책으로만 처리
        if (targetGrade == PlanGrade.BASIC) {
            throw new IllegalArgumentException("무료 플랜(BASIC) 전환은 '구독 취소' 기능을 이용해주세요.");
        }

        boolean isUpgrade = targetGrade.ordinal() > currentGrade.ordinal();

        if (isUpgrade) {
            if (request.billingKey() == null || request.billingKey().isBlank()) {
                throw new IllegalArgumentException("유료 플랜 변경에는 billingKey가 필요합니다.");
            }

            log.info("[SubscriptionService] 업그레이드 결제 요청 (userId: {}, {} -> {})", userId, currentGrade, targetGrade);
            ExternalPaymentPort.PaymentResult paymentResult = externalPaymentPort.requestSubscriptionPayment(
                    userId,
                    targetGrade.name(),
                    targetGrade.getMonthlyPrice(),
                    request.billingKey()
            );

            if (!paymentResult.success()) {
                throw new IllegalStateException("구독 결제가 실패하였습니다. 사유: " + paymentResult.errorMessage());
            }

            externalSubscriptionPort.changePlan(userId, targetGrade);
            LocalDateTime nextBillingDate = externalPaymentPort.getNextBillingDate(userId);

            return new SubscriptionChangeResultResponse(
                    targetGrade.name(),
                    null,
                    "ACTIVE",
                    nextBillingDate,
                    "업그레이드가 즉시 반영되었습니다."
            );
        }

        LocalDateTime nextBillingDate = externalPaymentPort.getNextBillingDate(userId);
        externalSubscriptionPort.schedulePlanDowngrade(userId, targetGrade, nextBillingDate);

        return new SubscriptionChangeResultResponse(
                currentGrade.name(),
                targetGrade.name(),
                "CHANGE_RESERVED",
                nextBillingDate,
                "다음 결제일에 다운그레이드가 반영됩니다."
        );
    }

    @Override
    @Transactional
    public SubscriptionChangeResultResponse cancelSubscription(Long userId) {
        validateUserId(userId);

        PlanGrade currentPlan = externalSubscriptionPort.getCurrentPlan(userId);
        if (currentPlan == PlanGrade.BASIC) {
            throw new IllegalStateException("이미 BASIC 플랜을 사용 중이므로 구독을 취소할 수 없습니다.");
        }

        LocalDateTime nextBillingDate = externalPaymentPort.getNextBillingDate(userId);
        externalSubscriptionPort.cancelSubscription(userId, nextBillingDate);

        return new SubscriptionChangeResultResponse(
                currentPlan.name(),
                PlanGrade.BASIC.name(),
                "CANCEL_RESERVED",
                nextBillingDate,
                "다음 결제일에 BASIC으로 전환됩니다."
        );
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }
    }
}