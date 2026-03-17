package com.fallguys.subscription.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
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
            nextBillingDate = externalSubscriptionPort.getNextBillingDate(userId);
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
            throw new BusinessException(ErrorCode.SUBSCRIPTION_INVALID_REQUEST);
        }

        PlanGrade targetGrade;
        try {
            targetGrade = PlanGrade.valueOf(request.targetPlanGrade().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_INVALID_PLAN);
        }

        PlanGrade currentGrade = externalSubscriptionPort.getCurrentPlan(userId);
        if (currentGrade == targetGrade) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_SAME_PLAN);
        }

        boolean isUpgrade = targetGrade.ordinal() > currentGrade.ordinal();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextBillingDate = resolveNextBillingDate(userId, now);

        if (isUpgrade) {
            if (request.billingKey() == null || request.billingKey().isBlank()) {
                throw new BusinessException(ErrorCode.SUBSCRIPTION_BILLING_KEY_REQUIRED);
            }

            ExternalPaymentPort.PaymentResult result = externalPaymentPort.requestSubscriptionPayment(
                    userId,
                    targetGrade.name(),
                    targetGrade.getMonthlyPrice(),
                    request.billingKey()
            );
            if (!result.success()) {
                log.warn("[Subscription] upgrade payment failed userId={}, code={}, msg={}",
                        userId, result.errorCode(), result.errorMessage());
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            externalSubscriptionPort.changePlan(userId, targetGrade);
            externalSubscriptionPort.saveBillingKey(userId, request.billingKey());

            nextBillingDate = computeNextMonthlyBillingDate(now);
            externalSubscriptionPort.setNextBillingDate(userId, nextBillingDate);

            return new SubscriptionChangeResultResponse(
                    targetGrade.name(),
                    null,
                    "ACTIVE",
                    nextBillingDate,
                    "Upgrade applied immediately. Billing runs on nextBillingDate."
            );
        }

        externalSubscriptionPort.changePlan(userId, targetGrade);

        if (targetGrade == PlanGrade.BASIC) {
            nextBillingDate = null;
            externalSubscriptionPort.setNextBillingDate(userId, null);
        } else {
            nextBillingDate = resolveOrComputeMonthlyBillingDate(userId, now);
        }

        return new SubscriptionChangeResultResponse(
                targetGrade.name(),
                null,
                "ACTIVE",
                nextBillingDate,
                targetGrade == PlanGrade.BASIC
                        ? "Plan changed to BASIC immediately."
                        : "Downgrade applied immediately."
        );
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_INVALID_REQUEST);
        }
    }

    private LocalDateTime resolveNextBillingDate(Long userId, LocalDateTime now) {
        LocalDateTime nextBillingDate = externalSubscriptionPort.getNextBillingDate(userId);
        if (nextBillingDate != null && nextBillingDate.isAfter(now)) {
            return nextBillingDate;
        }
        return null;
    }

    private LocalDateTime resolveOrComputeMonthlyBillingDate(Long userId, LocalDateTime now) {
        LocalDateTime nextBillingDate = externalSubscriptionPort.getNextBillingDate(userId);
        if (nextBillingDate != null && nextBillingDate.isAfter(now)) {
            return nextBillingDate;
        }
        LocalDateTime computed = computeNextMonthlyBillingDate(now);
        externalSubscriptionPort.setNextBillingDate(userId, computed);
        return computed;
    }

    private LocalDateTime computeNextMonthlyBillingDate(LocalDateTime now) {
        LocalDateTime nextMonth = now.plusMonths(1);
        return nextMonth.withHour(9).withMinute(0).withSecond(0).withNano(0);
    }
}
