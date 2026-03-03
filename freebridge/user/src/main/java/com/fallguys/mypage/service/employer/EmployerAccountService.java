package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerSubscriptionResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fallguys.mypage.api.web.dto.employer.request.UpdateSubscriptionRequestDto;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import org.springframework.transaction.annotation.Transactional;

import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.employer.request.UpdatePasswordRequestDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerNotificationSettingsDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerAccountService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmployerRepository employerRepository;
    private final SharedMypageApi sharedMypageApi;

    public void updatePassword(Long employerId, UpdatePasswordRequestDto request) {
        // TDD Green Phase: call external module api
        sharedMypageApi.updatePassword(request.newPassword());
    }

    @Transactional
    public void updateSubscription(Long userId, UpdateSubscriptionRequestDto request) {
        sharedMypageApi.updateSubscription(userId, request.targetPlan().toUpperCase());
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
