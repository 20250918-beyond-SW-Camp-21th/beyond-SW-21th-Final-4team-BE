package com.fallguys.mypage.api.shared;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerSubscriptionResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerNotificationSettingsDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerFreelancerSearchResponseDto;
import com.fallguys.user.api.shared.response.ExternalUserResponse;

public interface SharedMypageApi {

    /**
     * User 도메인에 비밀번호 변경을 요청하는 포트 인터페이스입니다.
     * (추후 FeignClient 방식으로 전환 예정)
     *
     * @param userId 변경할 대상 사용자의 고유 ID
     * @param currentPassword 현재 비밀번호
     * @param updatedPassword 변경할 새 비밀번호
     */
    void updatePassword(Long userId, String currentPassword, String updatedPassword);

    ExternalUserResponse getUserById(Long userId);

    void updateUserName(Long userId, String name);

    EmployerSubscriptionResponseDto getSubscription(Long userId);

    void updateSubscription(Long userId, String targetPlan);

    EmployerNotificationSettingsDto getNotificationSettings(Long userId);

    void updateNotificationSettings(Long userId, Boolean emailEnabled);

    EmployerFreelancerSearchResponseDto getEmployerFreelancers(int page, int size, String keyword);
}
