package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.api.web.dto.employer.request.UpdateSubscriptionRequestDto;
import java.util.Optional;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerSubscriptionResponseDto;
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
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private EmployerAccountService employerAccountService;

    @Test
    @DisplayName("고용주 구독 정보 조회: Redis에서 정보를 정상적으로 가져와 파싱한다")
    void getSubscription_Success() {
        // Given
        Long employerId = 1L;
        String redisKey = "employer:subscription:" + employerId;

        Map<String, Object> mockData = Map.of(
            "currentPlan", "PRIME",
            "features", List.of("인재풀 무제한 열람", "프로젝트 상단 노출", "수수료 면제"),
            "nextBillingDate", "2026-04-03T12:00:00"
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockData);

        // When
        EmployerSubscriptionResponseDto result = employerAccountService.getSubscription(employerId);

        // Then
        assertEquals("PRIME", result.currentPlan());
        assertEquals(3, result.features().size());
        assertEquals(LocalDateTime.of(2026, 4, 3, 12, 0, 0), result.nextBillingDate());
    }

    @Test
    @DisplayName("고용주 구독 정보 조회: Redis 값이 없으면 빈 껍데기 객체를 반환한다 (오류 방지)")
    void getSubscription_Empty() {
        // Given
        Long employerId = 2L;
        String redisKey = "employer:subscription:" + employerId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // When
        EmployerSubscriptionResponseDto result = employerAccountService.getSubscription(employerId);

        // Then
        assertNull(result.currentPlan());
        assertNull(result.features());
        assertNull(result.nextBillingDate());
    }
    @Mock
    private EmployerRepository employerRepository;

    @Test
    @DisplayName("구독 플랜 변경 신청: 정상적으로 엔티티의 Subscription 필드가 갱신된다")
    void updateSubscription_Success() {
        // Given
        Long employerId = 1L;
        UpdateSubscriptionRequestDto request = new UpdateSubscriptionRequestDto("PRIME");
        
        // Reflection 또는 Mock으로 Employer 객체 생성
        Employer mockEmployer = mock(Employer.class);

        when(employerRepository.findByUserId(employerId)).thenReturn(Optional.of(mockEmployer));

        // When
        employerAccountService.updateSubscription(employerId, request);

        // Then
        verify(mockEmployer, times(1)).changeSubscription(Subscription.PRIME);
    }

    @Test
    @DisplayName("구독 플랜 변경 신청: 존재하지 않는 회원인 경우 예외 또는 무시 처리")
    void updateSubscription_NotFound() {
        // Given
        Long employerId = 999L;
        UpdateSubscriptionRequestDto request = new UpdateSubscriptionRequestDto("PRO");

        when(employerRepository.findByUserId(employerId)).thenReturn(Optional.empty());

        // When
        try {
            employerAccountService.updateSubscription(employerId, request);
        } catch (IllegalArgumentException e) {
            assertEquals("해당 고용주를 찾을 수 없습니다.", e.getMessage());
        }

        // Then (changeSubscription should not be called since there is no entity)
        // verify no interactions
    }
}
