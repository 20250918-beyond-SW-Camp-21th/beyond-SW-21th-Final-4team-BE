package com.fallguys.subscription.service;

import com.fallguys.subscription.api.request.SubscriptionCancelRequest;
import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionResponse;
import com.fallguys.subscription.api.shared.ExternalSubscriptionPort;
import com.fallguys.subscription.entity.PlanGrade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구독 도메인 핵심 비즈니스 로직 구현체.
 *
 * <p>이 서비스는 mypage, user 등 타 도메인을 직접 import하지 않습니다.
 * 데이터 접근은 {@link ExternalSubscriptionPort}를 통해서만 이루어집니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final ExternalSubscriptionPort externalSubscriptionPort;

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

        log.info("[SubscriptionService] 구독 플랜 변경 요청 (userId: {}, {} -> {})", userId, currentGrade, targetGrade);
        externalSubscriptionPort.changePlan(userId, targetGrade);
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
