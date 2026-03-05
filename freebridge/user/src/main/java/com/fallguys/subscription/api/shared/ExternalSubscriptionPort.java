package com.fallguys.subscription.api.shared;

import com.fallguys.subscription.entity.PlanGrade;

import java.time.LocalDateTime;

/**
 * subscription 도메인이 외부(mypage 등)에서 Employer 구독 데이터를 읽어오기 위한 포트 인터페이스.
 * mypage(subscription 패키지 외부)에서 구현
 */
public interface ExternalSubscriptionPort {

    /**
     * userId로 Employer의 현재 구독 플랜을 조회
     *
     * @param userId 조회할 사용자의 고유 ID
     * @return 현재 PlanGrade (없으면 BASIC 반환)
     */
    PlanGrade getCurrentPlan(Long userId);

    /**
     * userId로 Employer의 현재 구독 플랜에 따른 매칭 수수료율(Fee Rate)을 조회
     *
     * @param userId 조회할 사용자의 고유 ID
     * @return 현재 수수료율 (퍼센트, 예: 10.0)
     */
    double getFeeRate(Long userId);

    LocalDateTime getNextBillingDate(Long userId);

    /**
     * userId로 Employer의 구독 플랜을 변경
     *
     * @param userId       변경 대상 사용자의 고유 ID
     * @param targetGrade  변경할 목표 플랜 등급
     */
    void changePlan(Long userId, PlanGrade targetGrade);

    void saveBillingKey(Long userId, String billingKey);

    void setNextBillingDate(Long userId, LocalDateTime nextBillingDate);

    /**
     * 다운그레이드 예약 (즉시 변경 아님).
     * 현재 결제 주기(당월) 말까지 기존 플랜을 유지하고,
     * 다음 결제일부터 targetGrade로 전환합니다.
     * (예: PRIME → PRO: 이번 달은 PRIME 유지, 다음 달부터 PRO 청구)
     *
     * @param userId       예약 대상 사용자의 고유 ID
     * @param targetGrade  다음 결제일에 적용할 목표 플랜 등급
     */
    void schedulePlanDowngrade(Long userId, PlanGrade targetGrade, LocalDateTime effectiveDate);

    /**
     * 구독을 취소하고 BASIC 플랜으로 전환 요청을 처리합니다.
     *
     * @param userId 고용주 회원 식별자
     */
    void cancelSubscription(Long userId, LocalDateTime effectiveDate);

    void applyPendingSubscription(Long userId);
}
