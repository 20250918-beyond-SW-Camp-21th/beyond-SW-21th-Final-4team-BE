package com.fallguys.mypage.service.freelancer;

import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.employer.request.UpdatePasswordRequestDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerNotificationSettingsDto;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.FreelancerGrade;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreelancerAccountServiceTest {

    @Mock
    private SharedMypageApi sharedMypageApi;

    @Mock
    private FreelancerRepository freelancerRepository;

    @InjectMocks
    private FreelancerAccountService freelancerAccountService;

    // ─── updatePassword ────────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 1. 비밀번호 변경: 정상 요청이면 SharedMypageApi.updatePassword를 호출한다")
    void updatePassword_Success() {
        Long userId = 1L;
        UpdatePasswordRequestDto request = new UpdatePasswordRequestDto("currentPass1!", "newPassword1!");

        freelancerAccountService.updatePassword(userId, request);

        verify(sharedMypageApi, times(1)).updatePassword(userId, "currentPass1!", "newPassword1!");
    }

    @Test
    @DisplayName("[TDD] 2. 비밀번호 변경: request가 null이면 IllegalArgumentException을 던진다")
    void updatePassword_NullRequest_ThrowsException() {
        assertThatThrownBy(() -> freelancerAccountService.updatePassword(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("모두 입력해야 합니다");
    }

    @Test
    @DisplayName("[TDD] 3. 비밀번호 변경: currentPassword가 공백이면 IllegalArgumentException을 던진다")
    void updatePassword_BlankCurrentPassword_ThrowsException() {
        UpdatePasswordRequestDto request = new UpdatePasswordRequestDto("  ", "newPassword1!");

        assertThatThrownBy(() -> freelancerAccountService.updatePassword(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("모두 입력해야 합니다");
    }

    @Test
    @DisplayName("[TDD] 4. 비밀번호 변경: newPassword가 blank이면 IllegalArgumentException을 던진다")
    void updatePassword_BlankNewPassword_ThrowsException() {
        UpdatePasswordRequestDto request = new UpdatePasswordRequestDto("currentPass1!", "");

        assertThatThrownBy(() -> freelancerAccountService.updatePassword(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("모두 입력해야 합니다");
    }

    // ─── getNotificationSettings ───────────────────────────────────

    @Test
    @DisplayName("[TDD] 5. 알림설정 조회: 프리랜서가 존재하면 알림 설정을 정상적으로 반환한다")
    void getNotificationSettings_Success() {
        Long userId = 10L;
        Freelancer freelancer = Freelancer.create(userId, "백엔드 개발", FreelancerGrade.JUNIOR);
        freelancer.updateNotificationSettings(true, false);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(freelancer));

        FreelancerNotificationSettingsDto result = freelancerAccountService.getNotificationSettings(userId);

        assertThat(result.requestNotificationEnabled()).isTrue();
        assertThat(result.contractNotificationEnabled()).isFalse();
    }

    @Test
    @DisplayName("[TDD] 6. 알림설정 조회: 프리랜서가 없으면 IllegalArgumentException을 던진다")
    void getNotificationSettings_NotFound_ThrowsException() {
        given(freelancerRepository.findByUserId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> freelancerAccountService.getNotificationSettings(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 프리랜서");
    }

    // ─── updateNotificationSettings ────────────────────────────────

    @Test
    @DisplayName("[TDD] 7. 알림설정 수정: 정상 요청이면 Freelancer 엔티티의 알림 설정이 변경된다")
    void updateNotificationSettings_Success() {
        Long userId = 20L;
        Freelancer freelancer = Freelancer.create(userId, "프론트 개발", FreelancerGrade.JUNIOR);
        freelancer.updateNotificationSettings(true, true);
        given(freelancerRepository.findByUserId(userId)).willReturn(Optional.of(freelancer));

        // 계약 알림을 끔
        FreelancerNotificationSettingsDto request = new FreelancerNotificationSettingsDto(true, false);
        freelancerAccountService.updateNotificationSettings(userId, request);

        assertThat(freelancer.getContractNotificationEnabled()).isFalse();
        assertThat(freelancer.getRequestNotificationEnabled()).isTrue();
    }

    @Test
    @DisplayName("[TDD] 8. 알림설정 수정: request가 null이면 IllegalArgumentException을 던진다")
    void updateNotificationSettings_NullRequest_ThrowsException() {
        assertThatThrownBy(() -> freelancerAccountService.updateNotificationSettings(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("알림 설정 요청 값이 없습니다");
    }

    @Test
    @DisplayName("[TDD] 9. 알림설정 수정: 프리랜서가 없으면 IllegalArgumentException을 던진다")
    void updateNotificationSettings_NotFound_ThrowsException() {
        given(freelancerRepository.findByUserId(99L)).willReturn(Optional.empty());

        FreelancerNotificationSettingsDto request = new FreelancerNotificationSettingsDto(false, false);
        assertThatThrownBy(() -> freelancerAccountService.updateNotificationSettings(99L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 프리랜서");
    }
}
