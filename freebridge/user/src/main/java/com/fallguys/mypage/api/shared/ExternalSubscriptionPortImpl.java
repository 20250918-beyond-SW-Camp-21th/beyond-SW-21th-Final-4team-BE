package com.fallguys.mypage.api.shared;

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
    @Transactional
    public void changePlan(Long userId, PlanGrade targetGrade) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고용주입니다. (userId: " + userId + ")"));

        employer.changeSubscription(toSubscriptionEnum(targetGrade));
        log.info("[ExternalSubscriptionPortImpl] 구독 플랜 변경 완료 (userId: {}, plan: {})", userId, targetGrade);
    }

    @Override
    @Transactional
    public void cancelSubscription(Long userId, LocalDateTime effectiveDate) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고용주입니다. (userId: " + userId + ")"));

        // Keep current paid plan until billing day, then switch to BASIC.
        employer.scheduleSubscriptionChange(Subscription.BASIC, effectiveDate);
        log.info("[ExternalSubscriptionPortImpl] 구독 취소 예약 완료 (userId: {}, effectiveDate: {})", userId, effectiveDate);
    }

    @Override
    @Transactional
    public void schedulePlanDowngrade(Long userId, PlanGrade targetGrade, LocalDateTime effectiveDate) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고용주입니다. (userId: " + userId + ")"));

        employer.scheduleSubscriptionChange(toSubscriptionEnum(targetGrade), effectiveDate);
        log.info("[ExternalSubscriptionPortImpl] 다운그레이드 예약 등록 (userId: {}, current: {}, target: {}, effectiveDate: {})",
                userId, employer.getSubscription(), targetGrade, effectiveDate);
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