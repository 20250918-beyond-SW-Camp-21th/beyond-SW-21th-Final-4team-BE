package com.fallguys.mypage.api.shared;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.subscription.api.shared.ExternalSubscriptionPort;
import com.fallguys.subscription.entity.PlanGrade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalSubscriptionPortImpl implements ExternalSubscriptionPort {

    private final EmployerRepository employerRepository;

    @Override
    @Transactional(readOnly = true)
    public PlanGrade getCurrentPlan(Long userId) {
        return employerRepository.findByUserId(userId)
                .map(employer -> toPlanGrade(employer.getSubscription()))
                .orElse(PlanGrade.BASIC);
    }

    @Override
    @Transactional(readOnly = true)
    public double getFeeRate(Long userId) {
        return getCurrentPlan(userId).getFeeRate();
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime getNextBillingDate(Long userId) {
        return employerRepository.findByUserId(userId)
                .map(Employer::getNextBillingDate)
                .orElse(null);
    }

    @Override
    @Transactional
    public void changePlan(Long userId, PlanGrade targetGrade) {
        Employer employer = getEmployerOrThrow(userId);

        employer.changeSubscription(toSubscriptionEnum(targetGrade));
        log.info("[ExternalSubscriptionPortImpl] 구독 플랜 변경 완료 (userId: {}, plan: {})", userId, targetGrade);
    }

    @Override
    @Transactional
    public void saveBillingKey(Long userId, String billingKey) {
        if (billingKey == null || billingKey.isBlank()) {
            throw new BusinessException(ErrorCode.SUBSCRIPTION_BILLING_KEY_REQUIRED);
        }
        Employer employer = getEmployerOrThrow(userId);
        employer.updateBillingKey(billingKey);
    }

    @Override
    @Transactional
    public void setNextBillingDate(Long userId, LocalDateTime nextBillingDate) {
        Employer employer = getEmployerOrThrow(userId);
        employer.updateNextBillingDate(nextBillingDate);
    }

    @Override
    @Transactional
    public void applyPendingSubscription(Long userId) {
        Employer employer = getEmployerOrThrow(userId);
        employer.applyPendingSubscription();
    }

    private Employer getEmployerOrThrow(Long userId) {
        return employerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.USER_NOT_FOUND
                ));
    }

    private PlanGrade toPlanGrade(Subscription subscription) {
        if (subscription == null) return PlanGrade.BASIC;
        return switch (subscription) {
            case BASIC -> PlanGrade.BASIC;
            case PRO -> PlanGrade.PRO;
            case PRIME -> PlanGrade.PRIME;
        };
    }

    private Subscription toSubscriptionEnum(PlanGrade planGrade) {
        return switch (planGrade) {
            case BASIC -> Subscription.BASIC;
            case PRO -> Subscription.PRO;
            case PRIME -> Subscription.PRIME;
        };
    }
}
