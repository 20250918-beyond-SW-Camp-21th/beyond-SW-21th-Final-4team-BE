package com.fallguys.subscription.api.request;

/**
 * 구독 취소 요청 DTO
 */
public record SubscriptionCancelRequest(
        String cancelReason  // 옵셔널, 리서치 목적
) {}
