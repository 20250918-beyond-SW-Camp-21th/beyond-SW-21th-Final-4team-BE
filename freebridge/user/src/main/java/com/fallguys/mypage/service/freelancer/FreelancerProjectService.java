package com.fallguys.mypage.service.freelancer;

import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerAppliedProjectListDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProjectStatusStatsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreelancerProjectService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 프리랜서의 프로젝트 상태별 통계 조회
     * Redis Key: freelancer:project:stats:{freelancerId}
     * Expected value: Map<String, Integer> { appliedProjects, inProgressProjects, completedProjects }
     */
    public FreelancerProjectStatusStatsDto getProjectStats(Long freelancerId) {
        String redisKey = "freelancer:project:stats:" + freelancerId;
        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            if (rawData == null) {
                return FreelancerProjectStatusStatsDto.empty();
            }
            @SuppressWarnings("unchecked")
            Map<String, ?> stats = (Map<String, ?>) rawData;
            return FreelancerProjectStatusStatsDto.from(stats);
        } catch (Exception e) {
            log.error("Failed to parse freelancer project stats from Redis for freelancerId: {}", freelancerId, e);
            return FreelancerProjectStatusStatsDto.empty();
        }
    }

    /**
     * 프리랜서의 지원/진행 프로젝트 목록 조회 (상태 필터링 지원)
     * Redis Key: freelancer:project:applied:{freelancerId}
     * Expected value: List<Map<String, Object>> { projectId, title, employerName, applyStatus, appliedAt }
     */
    public List<FreelancerAppliedProjectListDto> getMyProjects(Long freelancerId, String statusFilter) {
        String redisKey = "freelancer:project:applied:" + freelancerId;
        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            if (rawData == null) {
                return Collections.emptyList();
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) rawData;

            return rawList.stream()
                    .map(data -> {
                        try {
                            return toAppliedProjectDto(data);
                        } catch (Exception e) {
                            log.error("Failed to parse individual project item for freelancerId: {}", freelancerId, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(dto -> statusFilter == null || statusFilter.isBlank()
                            || Objects.equals(dto.applyStatus(), statusFilter))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to parse freelancer applied project list from Redis for freelancerId: {}", freelancerId, e);
            return Collections.emptyList();
        }
    }

    private FreelancerAppliedProjectListDto toAppliedProjectDto(Map<String, Object> data) {
        if (data == null) return null;
        Long projectId = data.get("projectId") != null
                ? Long.valueOf(data.get("projectId").toString()) : null;
        String title = data.get("title") != null ? data.get("title").toString() : null;
        String employerName = data.get("employerName") != null ? data.get("employerName").toString() : null;
        String applyStatus = data.get("applyStatus") != null ? data.get("applyStatus").toString() : null;
        Long appliedAt = data.get("appliedAt") != null
                ? Long.valueOf(data.get("appliedAt").toString()) : null;

        return new FreelancerAppliedProjectListDto(projectId, title, employerName, applyStatus, appliedAt);
    }
}
