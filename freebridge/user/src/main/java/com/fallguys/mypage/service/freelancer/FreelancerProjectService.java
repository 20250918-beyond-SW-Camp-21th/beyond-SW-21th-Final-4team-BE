package com.fallguys.mypage.service.freelancer;

import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProjectListDto;
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
     * 프리랜서의 프로젝트 목록 조회 (상태 필터링 지원)
     * Redis Key: freelancer:project:list:{freelancerId}
     * Expected value: List<Map<String, Object>> {
     *     projectId, title, employerName, projectStatus, description, budget, techStack, startDate, endDate
     * }
     */
    public List<FreelancerProjectListDto> getMyProjects(Long freelancerId, String statusFilter) {
        String redisKey = "freelancer:project:list:" + freelancerId;
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
                            return toProjectDto(data);
                        } catch (Exception e) {
                            log.error("Failed to parse individual project item for freelancerId: {}", freelancerId, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(dto -> statusFilter == null || statusFilter.isBlank()
                            || Objects.equals(dto.projectStatus(), statusFilter))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to parse freelancer project list from Redis for freelancerId: {}", freelancerId, e);
            return Collections.emptyList();
        }
    }

    private FreelancerProjectListDto toProjectDto(Map<String, Object> data) {
        if (data == null) return null;
        Long projectId = data.get("projectId") != null
                ? Long.valueOf(data.get("projectId").toString()) : null;
        String title = data.get("title") != null ? data.get("title").toString() : null;
        String employerName = data.get("employerName") != null ? data.get("employerName").toString() : null;
        String projectStatus = data.get("projectStatus") != null ? data.get("projectStatus").toString() : null;
        String description = data.get("description") != null ? data.get("description").toString() : null;
        Long budget = data.get("budget") != null
                ? Long.valueOf(data.get("budget").toString()) : null;
        @SuppressWarnings("unchecked")
        List<String> techStack = data.get("techStack") instanceof List<?> rawList
                ? rawList.stream().filter(Objects::nonNull).map(Object::toString).toList()
                : List.of();
        String startDate = data.get("startDate") != null ? data.get("startDate").toString() : null;
        String endDate = data.get("endDate") != null ? data.get("endDate").toString() : null;

        return new FreelancerProjectListDto(
                projectId,
                title,
                employerName,
                projectStatus,
                description,
                budget,
                techStack,
                startDate,
                endDate
        );
    }
}
