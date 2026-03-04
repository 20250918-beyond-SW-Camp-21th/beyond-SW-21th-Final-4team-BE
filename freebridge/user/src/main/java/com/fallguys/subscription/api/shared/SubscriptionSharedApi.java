package com.fallguys.subscription.api.shared;

import com.fallguys.subscription.entity.PlanGrade;

public interface SubscriptionSharedApi {

    /**
     * userId로 Employer의 현재 구독 플랜 등급을 반환
     * 구독 정보가 없으면 BASIC을 반환
     *
     * @param userId 조회할 사용자의 고유 ID
     * @return 현재 PlanGrade
     */
    PlanGrade getPlanGrade(Long userId);

    /**
     * userId로 Employer의 현재 수수료율을 반환
     *
     * @param userId 조회할 사용자의 고유 ID
     * @return 수수료율 (예: 10.0)
     */
    double getFeeRate(Long userId);
}
