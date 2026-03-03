package com.fallguys.mypage.service.freelancer;

import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerAppliedProjectListDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProjectStatusStatsDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreelancerProjectServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private FreelancerProjectService freelancerProjectService;

    // ─── getProjectStats ─────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 1. 프리랜서 프로젝트 통계 조회: Redis에 등록된 통계 정보가 있을 때 정상적으로 파싱하여 반환한다")
    void getProjectStats_Success() {
        // given
        Long freelancerId = 1L;
        String redisKey = "freelancer:project:stats:" + freelancerId;

        Map<String, Integer> mockStats = Map.of(
                "appliedProjects", 8,
                "inProgressProjects", 3,
                "completedProjects", 12
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockStats);

        // when
        FreelancerProjectStatusStatsDto result = freelancerProjectService.getProjectStats(freelancerId);

        // then
        assertThat(result.appliedProjects()).isEqualTo(8);
        assertThat(result.inProgressProjects()).isEqualTo(3);
        assertThat(result.completedProjects()).isEqualTo(12);

        verify(redisTemplate, times(1)).opsForValue();
        verify(valueOperations, times(1)).get(redisKey);
    }

    @Test
    @DisplayName("[TDD] 2. 프리랜서 프로젝트 통계 조회: Redis 값이 없거나 null인 경우 0으로 채워진 통계 DTO를 반환한다 (Fallback)")
    void getProjectStats_Empty() {
        // given
        Long freelancerId = 2L;
        String redisKey = "freelancer:project:stats:" + freelancerId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // when
        FreelancerProjectStatusStatsDto result = freelancerProjectService.getProjectStats(freelancerId);

        // then
        assertThat(result.appliedProjects()).isEqualTo(0);
        assertThat(result.inProgressProjects()).isEqualTo(0);
        assertThat(result.completedProjects()).isEqualTo(0);
    }

    // ─── getMyProjects ────────────────────────────────────────────

    @Test
    @DisplayName("[TDD] 3. 프리랜서 지원 프로젝트 목록 조회: Redis에서 목록을 정상적으로 파싱하고 상태 필터링까지 성공한다")
    void getMyProjects_SuccessAndFiltered() {
        // given
        Long freelancerId = 1L;
        String redisKey = "freelancer:project:applied:" + freelancerId;

        List<Map<String, Object>> mockList = List.of(
                Map.of("projectId", 201, "title", "Spring 백엔드 개발", "employerName", "ABC Corp",
                        "applyStatus", "심사중", "appliedAt", 1740000000000L),
                Map.of("projectId", 202, "title", "React 프론트개발", "employerName", "XYZ Inc",
                        "applyStatus", "합격", "appliedAt", 1740100000000L)
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockList);

        // when – "심사중" 필터
        List<FreelancerAppliedProjectListDto> result = freelancerProjectService.getMyProjects(freelancerId, "심사중");

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).projectId()).isEqualTo(201L);
        assertThat(result.get(0).applyStatus()).isEqualTo("심사중");
    }

    @Test
    @DisplayName("[TDD] 4. 프리랜서 지원 프로젝트 목록 조회: statusFilter가 null이면 전체 목록을 반환한다")
    void getMyProjects_NoFilter_ReturnsAll() {
        // given
        Long freelancerId = 1L;
        String redisKey = "freelancer:project:applied:" + freelancerId;

        List<Map<String, Object>> mockList = List.of(
                Map.of("projectId", 201, "title", "프로젝트A", "employerName", "A사", "applyStatus", "심사중", "appliedAt", 1000L),
                Map.of("projectId", 202, "title", "프로젝트B", "employerName", "B사", "applyStatus", "합격", "appliedAt", 2000L)
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockList);

        // when
        List<FreelancerAppliedProjectListDto> result = freelancerProjectService.getMyProjects(freelancerId, null);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("[TDD] 5. 프리랜서 지원 프로젝트 목록 조회: Redis 값이 없거나 null이면 빈 리스트를 반환한다")
    void getMyProjects_Empty() {
        // given
        Long freelancerId = 3L;
        String redisKey = "freelancer:project:applied:" + freelancerId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(null);

        // when
        List<FreelancerAppliedProjectListDto> result = freelancerProjectService.getMyProjects(freelancerId, null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[TDD] 6. 프리랜서 지원 프로젝트 목록 조회: Map 내에 applyStatus 키가 없어도 NPE 없이 null로 파싱한다")
    void getMyProjects_MissingApplyStatusKey_ReturnsGracefully() {
        // given
        Long freelancerId = 1L;
        String redisKey = "freelancer:project:applied:" + freelancerId;

        List<Map<String, Object>> mockList = List.of(
                Map.of("projectId", 301, "title", "키 누락 프로젝트", "employerName", "Z사", "appliedAt", 999L)
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(redisKey)).thenReturn(mockList);

        // when (null 필터 → 전체 조회)
        List<FreelancerAppliedProjectListDto> result = freelancerProjectService.getMyProjects(freelancerId, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).applyStatus()).isNull();
    }
}
