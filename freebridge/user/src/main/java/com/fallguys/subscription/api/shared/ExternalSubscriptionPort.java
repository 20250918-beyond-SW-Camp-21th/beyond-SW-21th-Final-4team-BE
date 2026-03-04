package com.fallguys.subscription.api.shared;

import com.fallguys.subscription.entity.PlanGrade;
import com.fallguys.subscription.entity.SubscriptionStatus;

import java.time.LocalDateTime;

/**
 * subscription 도메인이 외부(mypage 등)에서 Employer 구독 데이터를 읽어오기 위한 포트 인터페이스.
 * mypage(subscription 패키지 외부)에서 구현합니다.
 */
public interface ExternalSubscriptionPort {

    /**
     * userId로 Employer의 현재 구독 플랜을 조회합니다.
     *
     * @param userId 조회할 사용자의 고유 ID
     * @return 현재 PlanGrade (없으면 BASIC 반환)
     */
    PlanGrade getCurrentPlan(Long userId);

    /**
     * userId로 Employer의 구독 플랜을 변경합니다.
     *
     * @param userId       변경 대상 사용자의 고유 ID
     * @param targetGrade  변경할 목표 플랜 등급
     */
    void changePlan(Long userId, PlanGrade targetGrade);

    /**
     * userId로 Employer의 구독을 취소 예약합니다. (다음 결제일에 BASIC으로 전환)
     *
     * @param userId 취소 대상 사용자의 고유 ID
     * @param cancelReason 취소 이유 (옵셔널, 리서치용)
     */
    void cancelSubscription(Long userId, String cancelReason);
}
