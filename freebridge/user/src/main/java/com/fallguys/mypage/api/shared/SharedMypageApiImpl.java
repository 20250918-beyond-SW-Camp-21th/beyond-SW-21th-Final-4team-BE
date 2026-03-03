package com.fallguys.mypage.api.shared;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerSubscriptionResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerNotificationSettingsDto;

@Slf4j
@Component
public class SharedMypageApiImpl implements SharedMypageApi {

    @Override
    public void updatePassword(String updatedPassword) {
        log.info("SharedMypageApi: 외부 User 모듈로 비밀번호 변경(updatePassword) 요청이 전달되었습니다.");
    }

    @Override
    public EmployerSubscriptionResponseDto getSubscription(Long userId) {
        log.info("SharedMypageApi: 외부 모듈에 구독 정보 조회 요청 전달 (userId: {})", userId);
        return new EmployerSubscriptionResponseDto("BASIC", null, null);
    }

    @Override
    public void updateSubscription(Long userId, String targetPlan) {
        log.info("SharedMypageApi: 외부 모듈에 구독 변경 요청 전달 (userId: {}, plan: {})", userId, targetPlan);
    }

    @Override
    public EmployerNotificationSettingsDto getNotificationSettings(Long userId) {
        log.info("SharedMypageApi: 외부 모듈에 알림 설정 조회 요청 전달 (userId: {})", userId);
        return new EmployerNotificationSettingsDto(true);
    }

    @Override
    public void updateNotificationSettings(Long userId, Boolean emailEnabled) {
        log.info("SharedMypageApi: 외부 모듈에 알림 설정 변경 요청 전달 (userId: {}, emailEnabled: {})", userId, emailEnabled);
    }
}
