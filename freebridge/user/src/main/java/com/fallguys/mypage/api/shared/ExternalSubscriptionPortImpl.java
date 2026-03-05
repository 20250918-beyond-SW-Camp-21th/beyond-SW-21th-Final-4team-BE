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

/**
 * subscription 도메인의 {@link ExternalSubscriptionPort} 인터페이스를 구현하는 어댑터.
 * mypage 패키지 소속이지만, subscription 도메인이 Employer 데이터에
 * 직접 의존하지 않도록 anti-corruption layer 역할 수행
 */
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
        
        // 당월 말까지는 기존 프로/프라임 등급 유지, 다음 결제일에 BASIC으로 예약 전환
        employer.scheduleSubscriptionChange(Subscription.BASIC, effectiveDate);
        log.info("[ExternalSubscriptionPortImpl] 구독 취소 예약 완료 (userId: {}, 적용예정일: {})", userId, effectiveDate);
    }

    @Override
    @Transactional
    public void schedulePlanDowngrade(Long userId, PlanGrade targetGrade, LocalDateTime effectiveDate) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고용주입니다. (userId: " + userId + ")"));
        
        employer.scheduleSubscriptionChange(toSubscriptionEnum(targetGrade), effectiveDate);

        log.info("[ExternalSubscriptionPortImpl] 다운그레이드 예약 등록 (userId: {}, 현재: {}, 예약플랜: {}, 변경예정일: {})",
                userId, employer.getSubscription(), targetGrade, effectiveDate);
    }

    // ---- 변환 헬퍼 ----

    private PlanGrade toPlanGrade(Subscription subscription) {
        if (subscription == null) return PlanGrade.BASIC;
        return switch (subscription) {
            case BASIC -> PlanGrade.BASIC;
            case PRO   -> PlanGrade.PRO;
            case PRIME -> PlanGrade.PRIME;
        };
    }

    private Subscription toSubscriptionEnum(PlanGrade planGrade) {
        return switch (planGrade) {
            case BASIC -> Subscription.BASIC;
            case PRO   -> Subscription.PRO;
            case PRIME -> Subscription.PRIME;
        };
    }
}
