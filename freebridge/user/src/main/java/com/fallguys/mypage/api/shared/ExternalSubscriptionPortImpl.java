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
    @Transactional
    public void changePlan(Long userId, PlanGrade targetGrade) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고용주입니다. (userId: " + userId + ")"));
        employer.changeSubscription(toSubscriptionEnum(targetGrade));
        log.info("[ExternalSubscriptionPortImpl] 구독 플랜 변경 완료 (userId: {}, plan: {})", userId, targetGrade);
    }

    @Override
    @Transactional
    public void cancelSubscription(Long userId, String cancelReason) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고용주입니다. (userId: " + userId + ")"));
        // 취소 예약: BASIC으로 전환 (현재 테이블 구조에서는 즉시 BASIC 처리; 다음 결제일 관리 테이블 추가 시 개선)
        employer.changeSubscription(Subscription.BASIC);
        log.info("[ExternalSubscriptionPortImpl] 구독 취소 처리 완료 (userId: {}, reason: {})", userId, cancelReason);
    }

    @Override
    @Transactional
    public void schedulePlanDowngrade(Long userId, PlanGrade targetGrade) {
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고용주입니다. (userId: " + userId + ")"));
        
        // 다음 달 1일 오전 9시 정각으로 설정
        LocalDateTime effectiveDate = LocalDateTime.now()
                .plusMonths(1)
                .withDayOfMonth(1)
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        
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
