package com.fallguys.mypage.api.shared;

import com.fallguys.common.port.FileStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerFreelancerSearchItemDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerFreelancerSearchResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerSubscriptionResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerNotificationSettingsDto;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.user.api.shared.ExternalFreelancerSearchApi;
import com.fallguys.user.api.shared.ExternalUserApi;
import com.fallguys.user.api.shared.response.ExternalUserResponse;
import com.fallguys.user.api.shared.response.ExternalFreelancerSearchItem;
import com.fallguys.user.api.shared.response.ExternalFreelancerSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SharedMypageApiImpl implements SharedMypageApi {

    private final ExternalUserApi externalUserApi;
    private final EmployerRepository employerRepository;
    private final ExternalFreelancerSearchApi externalFreelancerSearchApi;
    private final FileStorage fileStorage;

    @Override
    public void updatePassword(Long userId, String currentPassword, String updatedPassword) {
        log.info("SharedMypageApi: 외부 User 모듈로 비밀번호 변경(updatePassword) 요청이 전달되었습니다. (userId: {})", userId);
        externalUserApi.updatePassword(userId, currentPassword, updatedPassword);
    }

    @Override
    public ExternalUserResponse getUserById(Long userId) {
        log.info("SharedMypageApi: 외부 User 모듈로 사용자 정보 조회 요청 전달 (userId: {})", userId);
        return externalUserApi.getUserById(userId);
    }

    @Override
    public void updateUserName(Long userId, String name) {
        log.info("SharedMypageApi: 외부 User 모듈로 이름 변경 요청 전달 (userId: {})", userId);
        externalUserApi.updateName(userId, name);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployerSubscriptionResponseDto getSubscription(Long userId) {
        log.info("SharedMypageApi: 내부 EmployerRepository를 통해 구독 정보 조회 (userId: {})", userId);
        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고용주입니다."));

        String planName = employer.getSubscription() != null ? employer.getSubscription().name() : "BASIC";
        return new EmployerSubscriptionResponseDto(planName, null, null);
    }

    @Override
    @Transactional
    public void updateSubscription(Long userId, String targetPlan) {
        log.info("SharedMypageApi: 내부 EmployerRepository를 통해 구독 변경 요청 (userId: {}, plan: {})", userId, targetPlan);

        if (targetPlan == null || targetPlan.isBlank()) {
            throw new IllegalArgumentException("Invalid subscription plan: '" + targetPlan + "'");
        }

        Employer employer = employerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 고용주입니다."));

        try {
            com.fallguys.mypage.entity.employer.Subscription subscription =
                com.fallguys.mypage.entity.employer.Subscription.valueOf(targetPlan.trim().toUpperCase());
            employer.changeSubscription(subscription);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid subscription plan: '" + targetPlan + "'", e);
        }
    }

    @Override
    public EmployerNotificationSettingsDto getNotificationSettings(Long userId) {
        log.info("SharedMypageApi: 외부 모듈에 알림 설정 조회 요청 전달 (userId: {})", userId);
        ExternalUserResponse response = externalUserApi.getUserById(userId);

        boolean isEmailEnabled = (response != null && response.getEmailEnabled() != null)
                                 ? response.getEmailEnabled()
                                 : false;

        return new EmployerNotificationSettingsDto(isEmailEnabled);
    }

    @Override
    public void updateNotificationSettings(Long userId, Boolean emailEnabled) {
        if (emailEnabled == null) {
            log.warn("SharedMypageApi: 알림 설정 변경 요청 실패 - emailEnabled 값이 null입니다. (userId: {})", userId);
            throw new IllegalArgumentException("이메일 알림 설정 값은 null일 수 없습니다.");
        }
        log.info("SharedMypageApi: 외부 모듈에 알림 설정 변경 요청 전달 (userId: {}, emailEnabled: {})", userId, emailEnabled);
        externalUserApi.updateEmailNotificationSetting(userId, emailEnabled);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployerFreelancerSearchResponseDto getEmployerFreelancers(int page, int size, String keyword) {
        ExternalFreelancerSearchResponse result = externalFreelancerSearchApi.searchFreelancers(page, size, keyword);
        List<EmployerFreelancerSearchItemDto> items = result.items().stream()
                .map(this::toItemDto)
                .toList();
        return new EmployerFreelancerSearchResponseDto(
                items,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }

    private EmployerFreelancerSearchItemDto toItemDto(ExternalFreelancerSearchItem item) {
        return new EmployerFreelancerSearchItemDto(
                item.freelancerId(),
                item.userId(),
                item.name(),
                item.job(),
                item.careerYears(),
                item.wage(),
                item.introduction(),
                toAccessibleUrl(item.avatarUrl()),
                item.skills() == null ? List.of() : item.skills(),
                item.grade()
        );
    }

    private String toAccessibleUrl(String storedKeyOrUrl) {
        if (storedKeyOrUrl == null || storedKeyOrUrl.isBlank()) {
            return null;
        }
        if (storedKeyOrUrl.startsWith("http://") || storedKeyOrUrl.startsWith("https://")) {
            return storedKeyOrUrl;
        }
        try {
            return fileStorage.generatePresignedUrl(storedKeyOrUrl);
        } catch (RuntimeException e) {
            log.error("Failed to generate accessible freelancer avatar URL. key={}", storedKeyOrUrl, e);
            return null;
        }
    }
}
