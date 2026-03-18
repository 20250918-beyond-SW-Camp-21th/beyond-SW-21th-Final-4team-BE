package com.fallguys.mypage.service.employer;

import com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectListResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectStatsResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerProjectService {

    private final RedisTemplate<String, Object> redisTemplate;

    public EmployerProjectStatsResponseDto getProjectStats(Long employerId) {
        String redisKey = "employer:project:stats:" + employerId;
        
        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            
            if (rawData == null) {
                return EmployerProjectStatsResponseDto.empty();
            }

            @SuppressWarnings("unchecked")
            Map<String, ?> stats = (Map<String, ?>) rawData;

            return EmployerProjectStatsResponseDto.from(stats);

        } catch (Exception e) {
            log.error("Failed to parse employer project stats from Redis for employerId: {}", employerId, e);
            return EmployerProjectStatsResponseDto.empty();
        }
    }

    public java.util.List<EmployerProjectListResponseDto> getMyProjects(Long employerId, String statusFilter) {
        String redisKey = "employer:project:list:" + employerId;
        
        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            
            if (rawData == null) {
                return java.util.Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> rawProjectList = (java.util.List<Map<String, Object>>) rawData;

            return rawProjectList.stream()
                    .map(com.fallguys.mypage.api.web.dto.employer.response.EmployerProjectListResponseDto::from)
                    .filter(java.util.Objects::nonNull)
                    .filter(dto -> statusFilter == null || statusFilter.isBlank() || java.util.Objects.equals(dto.status(), statusFilter))
                    .toList();

        } catch (Exception e) {
            log.error("Failed to parse employer project list from Redis for employerId: {}", employerId, e);
            return java.util.Collections.emptyList();
        }
    }

    public java.util.List<com.fallguys.mypage.api.web.dto.employer.response.EmployerApplicantStatusResponseDto> getApplicantStatus(Long employerId, Long projectId) {
        // TODO: In a real DB scenario, verify that the project actually belongs to employerId
        String redisKey = "employer:project:applicants:" + projectId;
        
        try {
            Object rawData = redisTemplate.opsForValue().get(redisKey);
            
            if (rawData == null) {
                return java.util.Collections.emptyList();
            }

            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> rawApplicantList = (java.util.List<Map<String, Object>>) rawData;

            return rawApplicantList.stream()
                    .map(data -> new com.fallguys.mypage.api.web.dto.employer.response.EmployerApplicantStatusResponseDto(
                            data.get("freelancerId") != null ? Long.valueOf(data.get("freelancerId").toString()) : null,
                            data.get("applyStatus") != null ? data.get("applyStatus").toString() : null
                    ))
                    .filter(dto -> dto.freelancerId() != null)
                    .toList();

        } catch (Exception e) {
            log.error("Failed to parse applicant status list from Redis for projectId: {}", projectId, e);
            return java.util.Collections.emptyList();
        }
    }
}
