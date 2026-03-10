package com.fallguys.mypage.service.freelancer;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerNotificationSettingsDto;
import com.fallguys.mypage.api.web.dto.employer.request.UpdatePasswordRequestDto;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FreelancerAccountService {

    private final SharedMypageApi sharedMypageApi;
    private final FreelancerRepository freelancerRepository;

    // ─── 비밀번호 변경 ─────────────────────────────────────────────

    public void updatePassword(Long userId, UpdatePasswordRequestDto request) {
        if (request == null
                || request.currentPassword() == null || request.currentPassword().isBlank()
                || request.newPassword() == null     || request.newPassword().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        sharedMypageApi.updatePassword(userId, request.currentPassword(), request.newPassword());
    }

    // ─── 알림 설정 조회 ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FreelancerNotificationSettingsDto getNotificationSettings(Long userId) {
        Freelancer freelancer = freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return new FreelancerNotificationSettingsDto(
                Boolean.TRUE.equals(freelancer.getRequestNotificationEnabled()),
                Boolean.TRUE.equals(freelancer.getContractNotificationEnabled())
        );
    }

    // ─── 알림 설정 수정 ─────────────────────────────────────────────

    @Transactional
    public void updateNotificationSettings(Long userId, FreelancerNotificationSettingsDto request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Freelancer freelancer = freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        freelancer.updateNotificationSettings(
                request.requestNotificationEnabled(),
                request.contractNotificationEnabled()
        );
    }
}
