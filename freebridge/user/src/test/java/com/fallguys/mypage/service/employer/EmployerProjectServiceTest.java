package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerApplicantStatusResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectStatsResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployerProjectServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private EmployerProjectService employerProjectService;

    @Test
    @DisplayName("고용주 프로젝트 통계 조회: Redis에 등록된 통계 정보가 있을 때 정상적으로 파싱하여 반환한다")
    void getProjectStats_Success() {
        // Given
        Long employerId = 1L;
        String redisKey = "employer:project:stats:" + employerId;

        Map<String, Long> mockStats = Map.of(   // Redis/Jackson은 정수를 Long으로 역직렬화함
            "totalProjects", 10L,
            "activeApplicants", 5L,
            "contractedFreelancers", 15L
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockStats);

        // When
        EmployerProjectStatsResponseDto result = employerProjectService.getProjectStats(employerId);

        // Then
        assertEquals(10, result.totalProjects());
        assertEquals(5, result.activeApplicants());
        assertEquals(15, result.contractedFreelancers());

        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(redisKey);
    }

    @Test
    @DisplayName("고용주 프로젝트 통계 조회: Redis 값이 없거나 null인 경우 0으로 채워진 통계 DTO를 반환한다 (Fallback)")
    void getProjectStats_Empty() {
        // Given
        Long employerId = 2L;
        String redisKey = "employer:project:stats:" + employerId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // When
        EmployerProjectStatsResponseDto result = employerProjectService.getProjectStats(employerId);

        // Then
        assertEquals(0, result.totalProjects());
        assertEquals(0, result.activeApplicants());
        assertEquals(0, result.contractedFreelancers());
    }

    @Test
    @DisplayName("고용주 프로젝트 목록 조회: Redis에서 목록을 정상적으로 파싱하고 상태 필터링까지 성공한다")
    void getMyProjects_Success_And_Filtered() {
        // Given
        Long employerId = 1L;
        String redisKey = "employer:project:list:" + employerId;

        java.util.List<Map<String, Object>> mockRedisList = java.util.List.of(
            Map.of("projectId", 101, "title", "A", "status", "모집중", "applicantCount", 5, "createdAt", "2026-03-01T10:00:00", "deadline", "2026-03-15T23:59:59"),
            Map.of("projectId", 102, "title", "B", "status", "진행중", "applicantCount", 12, "createdAt", "2026-02-15T09:00:00", "deadline", "2026-02-28T23:59:59")
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockRedisList);

        // When
        java.util.List<com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectListResponseDto> result = employerProjectService.getMyProjects(employerId, "모집중");

        // Then
        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).projectId());
        assertEquals("모집중", result.get(0).status());
    }

    @Test
    @DisplayName("고용주 프로젝트 목록 조회: Redis Map 내에 status 키가 없을 때 NPE 없이 안전하게 처리된다")
    void getMyProjects_MissingStatusKey_ReturnsGracefully() {
        // Given
        Long employerId = 1L;
        String redisKey = "employer:project:list:" + employerId;

        // "status" 키가 없는 Map
        java.util.List<Map<String, Object>> mockRedisList = java.util.List.of(
            Map.of("projectId", 101, "title", "A", "applicantCount", 5, "createdAt", "2026-03-01T10:00:00", "deadline", "2026-03-15T23:59:59")
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockRedisList);

        // When (statusFilter를 null로 주어 필터링 없이 전체 조회 시도)
        java.util.List<com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectListResponseDto> result = employerProjectService.getMyProjects(employerId, null);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("특정 프로젝트 지원자 현황 조회: Redis 값이 없거나 null이면 빈 리스트를 반환한다")
    void getMyProjects_Empty() {
        // Given
        Long employerId = 2L;
        String redisKey = "employer:project:list:" + employerId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // When
        java.util.List<com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectListResponseDto> result = employerProjectService.getMyProjects(employerId, null);

        // Then
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("특정 프로젝트 지원자 현황 조회: Redis에서 지원자 목록을 정상적으로 파싱하여 반환한다")
    void getApplicantStatus_Success() {
        // Given
        Long projectId = 101L;
        String redisKey = "employer:project:applicants:" + projectId;

        java.util.List<Map<String, Object>> mockRedisList = java.util.List.of(
            Map.of("freelancerId", 1, "applyStatus", "검토중"),
            Map.of("freelancerId", 2, "applyStatus", "면접")
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockRedisList);

        // When
        java.util.List<EmployerApplicantStatusResponseDto> result = employerProjectService.getApplicantStatus(1L, projectId);

        // Then
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).freelancerId());
        assertEquals("검토중", result.get(0).applyStatus());
        assertEquals(2L, result.get(1).freelancerId());
        assertEquals("면접", result.get(1).applyStatus());
    }

    @Test
    @DisplayName("특정 프로젝트 지원자 현황 조회: Redis Map 내에 applyStatus 키가 없을 때 NPE 없이 null로 파싱된다")
    void getApplicantStatus_MissingApplyStatusKey_ReturnsGracefully() {
        // Given
        Long projectId = 101L;
        String redisKey = "employer:project:applicants:" + projectId;

        // "applyStatus" 키가 없는 Map
        java.util.List<Map<String, Object>> mockRedisList = java.util.List.of(
            Map.of("freelancerId", 1)
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockRedisList);

        // When
        java.util.List<EmployerApplicantStatusResponseDto> result = employerProjectService.getApplicantStatus(1L, projectId);

        // Then
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).freelancerId());
        assertNull(result.get(0).applyStatus());
    }

    @Test
    @DisplayName("특정 프로젝트 지원자 현황 조회: Redis 값이 없거나 null이면 빈 리스트를 반환한다")
    void getApplicantStatus_Empty() {
        // Given
        Long projectId = 102L;
        String redisKey = "employer:project:applicants:" + projectId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // When
        java.util.List<EmployerApplicantStatusResponseDto> result = employerProjectService.getApplicantStatus(1L, projectId);

        // Then
        assertEquals(0, result.size());
    }
}
