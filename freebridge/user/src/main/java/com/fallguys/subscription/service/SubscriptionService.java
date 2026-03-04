package com.fallguys.subscription.service;

import com.fallguys.subscription.api.request.SubscriptionCancelRequest;
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
     * userId (고용주)의 구독을 취소합니다.
     * 취소 예약 처리되며, 다음 결제일부터 BASIC으로 전환됩니다.
     *
     * @param userId  요청자 사용자 ID
     * @param request 취소 요청 정보 (사유 포함)
     */
    void cancelSubscription(Long userId, SubscriptionCancelRequest request);
}
