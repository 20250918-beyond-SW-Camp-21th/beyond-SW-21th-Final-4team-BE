package com.fallguys.subscription.service;

import com.fallguys.subscription.api.shared.ExternalSubscriptionPort;
import com.fallguys.subscription.api.shared.SubscriptionSharedApi;
import com.fallguys.subscription.entity.PlanGrade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * subscription 도메인이 외부에 구독 정보를 제공하는 공유 API 구현체.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionSharedApiImpl implements SubscriptionSharedApi {

    private final ExternalSubscriptionPort externalSubscriptionPort;

    @Override
    public PlanGrade getPlanGrade(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("SubscriptionSharedApiImpl.getPlanGrade: userId must not be null");
        }
        return externalSubscriptionPort.getCurrentPlan(userId);
    }

    @Override
    public double getFeeRate(Long userId) {
        return getPlanGrade(userId).getFeeRate();
    }
}
