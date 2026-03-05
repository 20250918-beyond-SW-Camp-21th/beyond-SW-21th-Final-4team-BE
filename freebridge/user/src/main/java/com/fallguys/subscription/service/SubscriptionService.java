package com.fallguys.subscription.service;

import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionResponse;

/**
 * 구독 도메인 핵심 비즈니스 로직 인터페이스
 */
public interface SubscriptionService {

    /**
     * userId (고용주)의 현재 구독 정보를 조회합니다.
     *
     * @param userId 요청자 사용자 ID
     * @return 구독 정보 응답 DTO
     */
    SubscriptionResponse getSubscription(Long userId);

    /**
     * userId (고용주)의 구독 플랜을 변경합니다.
     *
     * @param userId  요청자 사용자 ID
     * @param request 변경할 플랜 정보
     */
    void changePlan(Long userId, SubscriptionChangeRequest request);

    /**
     * 유료 구독을 취소하고 자동 결제를 해지합니다.
     * 취소 시 즉시 BASIC 플랜으로 전환됩니다.
     *
     * @param userId 취소할 고용주의 사용자 ID
     * @throws IllegalStateException 이미 BASIC 플랜인 경우 발생
     */
    void cancelSubscription(Long userId);
}
