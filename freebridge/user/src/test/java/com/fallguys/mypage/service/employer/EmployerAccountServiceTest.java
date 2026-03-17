package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerSubscriptionResponseDto;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.api.web.dto.employer.request.UpdateSubscriptionRequestDto;
import com.fallguys.mypage.api.shared.SharedMypageApi;
import com.fallguys.mypage.api.web.dto.employer.request.UpdatePasswordRequestDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerSubscriptionResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerNotificationSettingsDto;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployerAccountServiceTest {

    @Mock
    private EmployerRepository employerRepository;

    @Mock
    private SharedMypageApi sharedMypageApi;

    @InjectMocks
    private EmployerAccountService employerAccountService;

    @Test
    @DisplayName("고용주 구독 정보 조회: SharedMypageApi를 통해 정상적으로 구독 정보를 가져온다")
    void getSubscription_Success() {
        // Given
        Long employerId = 1L;
        EmployerSubscriptionResponseDto mockResponse = new EmployerSubscriptionResponseDto(
            "PRIME",
            List.of("인재풀 무제한 열람", "프로젝트 상단 노출", "수수료 면제"),
            LocalDateTime.of(2026, 4, 3, 12, 0, 0)
        );

        when(sharedMypageApi.getSubscription(employerId)).thenReturn(mockResponse);

        // When
        EmployerSubscriptionResponseDto result = employerAccountService.getSubscription(employerId);

        // Then
        assertEquals("PRIME", result.currentPlan());
        assertEquals(3, result.features().size());
        assertEquals(LocalDateTime.of(2026, 4, 3, 12, 0, 0), result.nextBillingDate());
        verify(sharedMypageApi, times(1)).getSubscription(employerId);
    }

    @Test
    @DisplayName("고용주 구독 정보 조회: 구독 정보가 없을 경우 null 또는 빈 객체를 반환한다")
    void getSubscription_Empty() {
        // Given
        Long employerId = 2L;

        when(sharedMypageApi.getSubscription(employerId)).thenReturn(new EmployerSubscriptionResponseDto(null, null, null));

        // When
        EmployerSubscriptionResponseDto result = employerAccountService.getSubscription(employerId);

        // Then
        assertNull(result.currentPlan());
        assertNull(result.features());
        assertNull(result.nextBillingDate());
        verify(sharedMypageApi, times(1)).getSubscription(employerId);
    }

    @Test
    @DisplayName("구독 플랜 변경 신청: SharedMypageApi의 updateSubscription가 정상적으로 호출된다")
    void updateSubscription_Success() {
        // Given
        Long employerId = 1L;
        UpdateSubscriptionRequestDto request = new UpdateSubscriptionRequestDto("PRIME","","");

        // When
        employerAccountService.updateSubscription(employerId, request);

        // Then
        verify(sharedMypageApi, times(1)).updateSubscription(employerId, "PRIME");
    }

    @Test
    @DisplayName("구독 플랜 변경 신청: 소문자 입력 시 대문자로 변환되어 SharedMypageApi에 정상 전달된다")
    void updateSubscription_LowercaseInput_Success() {
        // Given
        Long employerId = 1L;
        UpdateSubscriptionRequestDto request = new UpdateSubscriptionRequestDto("prime","","");

        // When
        employerAccountService.updateSubscription(employerId, request);

        // Then
        verify(sharedMypageApi, times(1)).updateSubscription(employerId, "PRIME");
    }

    @Test
    @DisplayName("비밀번호 변경 신청: SharedMypageApi의 updatePassword가 정상적으로 호출된다")
    void updatePassword_Success() {
        // Given
        Long employerId = 1L;
        String newPassword = "newPassword123!";
        UpdatePasswordRequestDto request = new UpdatePasswordRequestDto("oldPassword", newPassword);

        // When
        employerAccountService.updatePassword(employerId, request);

        // Then
        verify(sharedMypageApi, times(1)).updatePassword(employerId, "oldPassword", newPassword);
    }

    @Test
    @DisplayName("알림 설정 조회: SharedMypageApi를 통해 알림 설정을 정상적으로 조회한다")
    void getNotificationSettings_Success() {
        // Given
        Long employerId = 1L;
        EmployerNotificationSettingsDto mockDto = new EmployerNotificationSettingsDto(true);
        when(sharedMypageApi.getNotificationSettings(employerId)).thenReturn(mockDto);

        // When
        EmployerNotificationSettingsDto result = employerAccountService.getNotificationSettings(employerId);

        // Then
        assertEquals(true, result.emailEnabled());
        verify(sharedMypageApi, times(1)).getNotificationSettings(employerId);
    }
}
