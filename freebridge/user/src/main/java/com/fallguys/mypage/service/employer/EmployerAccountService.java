package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerSubscriptionResponseDto;
import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionChangeResultResponse;
import com.fallguys.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


import com.fallguys.mypage.api.web.dto.employer.request.UpdateSubscriptionRequestDto;
import org.springframework.transaction.annotation.Transactional;

import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.employer.request.UpdatePasswordRequestDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerNotificationSettingsDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerAccountService {

    private final SharedMypageApi sharedMypageApi;
    private final SubscriptionService subscriptionService;

    public void updatePassword(Long employerId, UpdatePasswordRequestDto request) {
        if (request == null || 
            request.currentPassword() == null || request.currentPassword().isBlank() ||
            request.newPassword() == null || request.newPassword().isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호와 새 비밀번호를 모두 입력해야 합니다.");
        }
        // TDD Green Phase: call external module api
        sharedMypageApi.updatePassword(employerId, request.currentPassword(), request.newPassword());
    }

    @Transactional
    public SubscriptionChangeResultResponse updateSubscription(Long userId, UpdateSubscriptionRequestDto request) {
        if (request == null || request.targetPlan() == null || request.targetPlan().isBlank()) {
            throw new IllegalArgumentException("변경할 구독 플랜 값이 필요합니다.");
        }
        return subscriptionService.changePlan(
                userId,
                new SubscriptionChangeRequest(
                        request.targetPlan().toUpperCase(),
                        request.billingKey(),
                        request.paymentId()
                )
        );
    }

    public EmployerSubscriptionResponseDto getSubscription(Long userId) {
        return sharedMypageApi.getSubscription(userId);
    }

    public EmployerNotificationSettingsDto getNotificationSettings(Long userId) {
        return sharedMypageApi.getNotificationSettings(userId);
    }

    public void updateNotificationSettings(Long userId, Boolean emailEnabled) {
        sharedMypageApi.updateNotificationSettings(userId, emailEnabled);
    }
}
