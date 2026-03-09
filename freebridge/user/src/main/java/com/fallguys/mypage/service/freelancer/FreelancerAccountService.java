package com.fallguys.mypage.service.freelancer;

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
                || request.newPassword() == null || request.newPassword().isBlank()) {
            throw new IllegalArgumentException("현재 비밀번호와 새 비밀번호를 모두 입력해야 합니다.");
        }
        sharedMypageApi.updatePassword(userId, request.currentPassword(), request.newPassword());
    }

    // ─── 알림 설정 조회 ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FreelancerNotificationSettingsDto getNotificationSettings(Long userId) {
        Freelancer freelancer = freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프리랜서입니다."));
        return new FreelancerNotificationSettingsDto(
                Boolean.TRUE.equals(freelancer.getRequestNotificationEnabled()),
                Boolean.TRUE.equals(freelancer.getContractNotificationEnabled()));
    }

    // ─── 알림 설정 수정 ─────────────────────────────────────────────

    @Transactional
    public void updateNotificationSettings(Long userId, FreelancerNotificationSettingsDto request) {
        if (request == null) {
            throw new IllegalArgumentException("알림 설정 요청 값이 없습니다.");
        }
        Freelancer freelancer = freelancerRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프리랜서입니다."));
        freelancer.updateNotificationSettings(
                request.requestNotificationEnabled(),
                request.contractNotificationEnabled());
    }
}
