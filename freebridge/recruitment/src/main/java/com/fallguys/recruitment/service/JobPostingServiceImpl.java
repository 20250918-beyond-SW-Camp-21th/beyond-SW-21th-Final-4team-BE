package com.fallguys.recruitment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.AiRecommendationResponseDTO;
import com.fallguys.recruitment.api.dto.response.EmployerProjectSearchDTO;
import com.fallguys.recruitment.api.dto.response.FreelancerJobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.entity.JobPostingFavorite;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.repository.JobPostingFavoriteRepo;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import com.fallguys.recruitment.service.port.RecruitmentUser;
import com.fallguys.recruitment.service.port.RecruitmentUserReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostingServiceImpl implements JobPostingService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final String CACHE_PREFIX = "recruitment";
    private static final int SCAN_BATCH_SIZE = 1000;

    private final JobPostingRepo jobPostingRepo;
    private final JobPostingFavoriteRepo jobPostingFavoriteRepo;
    private final ProjectPostingRepo projectPostingRepo;
    private final RecruitmentUserReader recruitmentUserReader;
    private final RecommendationEngine recommendationEngine; // AiAdapter 주입
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<JobPostingSearchDTO> getJobPostings(Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        String cacheKey = employerJobsCacheKey(user.id());

        List<JobPostingSearchDTO> cached = readCache(cacheKey, new TypeReference<>() {});
        if (cached != null) {
            return cached;
        }

        List<JobPostingSearchDTO> loaded = jobPostingRepo.findAllByEmployerIdAndStatusNot(user.id(), Status.DELETED)
                .stream()
                .map(this::toJobPostingSearchDto)
                .toList();
        writeCache(cacheKey, loaded);
        return loaded;
    }

    @Override
    @Transactional
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        JobPosting jobPosting = JobPosting.from(jobPostingCreateDTO, user.id(), user.name());
        jobPostingRepo.save(jobPosting);
        runAfterCommit(() -> evictEmployerSideCaches(user.id()));
    }

    @Override
    @Transactional
    public void updateJobPosting(JobPostingUpdateDTO jobPostingUpdateDTO, Long jobPostingId, Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateOwnership(jobPosting, user.id());
        validateNotDeleted(jobPosting);
        try {
            jobPosting.update(jobPostingUpdateDTO);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        runAfterCommit(() -> evictEmployerSideCaches(user.id()));
    }

    @Override
    @Transactional
    public void deleteJobPosting(Long jobPostingId, Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateOwnership(jobPosting, user.id());
        validateNotDeleted(jobPosting);
        jobPosting.delete();
        runAfterCommit(() -> evictEmployerSideCaches(user.id()));
    }

    @Override
    public List<JobPostingSearchDTO> getAllJobPostings() {
        String cacheKey = allJobsCacheKey();
        List<JobPostingSearchDTO> cached = readCache(cacheKey, new TypeReference<>() {});
        if (cached != null) {
            return cached;
        }

        List<JobPostingSearchDTO> loaded = jobPostingRepo.findAllByStatusNot(Status.DELETED)
                .stream()
                .map(this::toJobPostingSearchDto)
                .toList();
        writeCache(cacheKey, loaded);
        return loaded;
    }

    @Override
    public List<EmployerProjectSearchDTO> getEmployerProjects(Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        String cacheKey = employerProjectsCacheKey(user.id());
        List<EmployerProjectSearchDTO> cached = readCache(cacheKey, new TypeReference<>() {});
        if (cached != null) {
            return cached;
        }

        List<EmployerProjectSearchDTO> loaded = projectPostingRepo.findAllByEmployerIdOrderByCreatedAtDesc(user.id())
                .stream()
                .map(this::toEmployerProjectSearchDto)
                .toList();
        writeCache(cacheKey, loaded);
        return loaded;
    }

    @Override
    public List<FreelancerJobPostingSearchDTO> searchJobPostingsForFreelancer(Long userId, String keyword, boolean favoritesOnly) {
        RecruitmentUser user = recruitmentUserReader.getFreelancerByIdOrThrow(userId);
        Long freelancerId = user.id();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String cacheKey = freelancerSearchCacheKey(freelancerId, normalizedKeyword, favoritesOnly);

        List<FreelancerJobPostingSearchDTO> cached = readCache(cacheKey, new TypeReference<>() {});
        if (cached != null) {
            return cached;
        }

        Set<Long> favoriteJobPostingIds = new HashSet<>(
                jobPostingFavoriteRepo.findAllByFreelancerId(freelancerId)
                        .stream()
                        .map(JobPostingFavorite::getJobPostingId)
                        .toList()
        );

        List<FreelancerJobPostingSearchDTO> loaded = jobPostingRepo.findAllByStatusAndPostingStatusIn(
                        Status.ACTIVE,
                        EnumSet.of(JobPostingStatus.OPEN, JobPostingStatus.IN_PROGRESS)
                )
                .stream()
                .filter(jobPosting -> matchesKeyword(jobPosting, normalizedKeyword))
                .filter(jobPosting -> !favoritesOnly || favoriteJobPostingIds.contains(jobPosting.getId()))
                .map(jobPosting -> toFreelancerSearchDto(jobPosting, favoriteJobPostingIds.contains(jobPosting.getId())))
                .toList();
        writeCache(cacheKey, loaded);
        return loaded;
    }

    @Override
    @Transactional
    public void addFavoriteJobPosting(Long userId, Long jobPostingId) {
        RecruitmentUser user = recruitmentUserReader.getFreelancerByIdOrThrow(userId);
        Long freelancerId = user.id();

        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateNotDeleted(jobPosting);

        try {
            jobPostingFavoriteRepo.save(JobPostingFavorite.of(freelancerId, jobPostingId));
        } catch (DataIntegrityViolationException ignored) {
            // Duplicate favorite is treated as idempotent no-op.
        }
        runAfterCommit(() -> evictFreelancerSearchCaches(freelancerId));
    }

    @Override
    @Transactional
    public void removeFavoriteJobPosting(Long userId, Long jobPostingId) {
        RecruitmentUser user = recruitmentUserReader.getFreelancerByIdOrThrow(userId);
        Long freelancerId = user.id();

        jobPostingFavoriteRepo.deleteByFreelancerIdAndJobPostingId(freelancerId, jobPostingId);
        runAfterCommit(() -> evictFreelancerSearchCaches(freelancerId));
    }

    private JobPosting getJobPostingOrThrow(Long jobPostingId) {
        return jobPostingRepo.findById(jobPostingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND));
    }

    private void validateOwnership(JobPosting jobPosting, Long userId) {
        if (!jobPosting.getEmployerId().equals(userId)) {
            throw new BusinessException(ErrorCode.JOB_POSTING_FORBIDDEN);
        }
    }

    private void validateNotDeleted(JobPosting jobPosting) {
        if (jobPosting.getStatus() == Status.DELETED) {
            throw new BusinessException(ErrorCode.JOB_POSTING_ALREADY_DELETED);
        }
    }

    private JobPostingSearchDTO toJobPostingSearchDto(JobPosting jobPosting) {
        return new JobPostingSearchDTO(
                jobPosting.getId(),
                jobPosting.getEmployerName(),
                jobPosting.getTitle(),
                jobPosting.getDescription(),
                new ArrayList<>(jobPosting.getTechStack()),
                jobPosting.getBudget(),
                jobPosting.getDuration(),
                jobPosting.getHeadcount(),
                jobPosting.getMatchedHeadcount(),
                jobPosting.getPostingStatus()
        );
    }

    private FreelancerJobPostingSearchDTO toFreelancerSearchDto(JobPosting jobPosting, boolean favorite) {
        return new FreelancerJobPostingSearchDTO(
                jobPosting.getId(),
                jobPosting.getEmployerName(),
                jobPosting.getTitle(),
                jobPosting.getDescription(),
                new ArrayList<>(jobPosting.getTechStack()),
                jobPosting.getBudget(),
                jobPosting.getDuration(),
                jobPosting.getHeadcount(),
                jobPosting.getMatchedHeadcount(),
                favorite
        );
    }

    private EmployerProjectSearchDTO toEmployerProjectSearchDto(Project project) {
        return new EmployerProjectSearchDTO(
                project.getId(),
                project.getJobPosting().getId(),
                project.getFreelancerId(),
                project.getProjectName(),
                project.getHeadcount(),
                project.getStartDate(),
                project.getEndDate(),
                project.getStatus()
        );
    }

    private boolean matchesKeyword(JobPosting jobPosting, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }

        if (containsIgnoreCase(jobPosting.getTitle(), keyword) || containsIgnoreCase(jobPosting.getDescription(), keyword)) {
            return true;
        }

        return jobPosting.getTechStack().stream()
                .anyMatch(tech -> containsIgnoreCase(tech, keyword));
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(keyword);
    }

    @Override   // 기업용
    public List<AiRecommendationResponseDTO> getRecommendedFreelancers(Long jobPostingId, Long userId) {
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);

        validateNotDeleted(jobPosting);

        validateOwnership(jobPosting, userId);

        return recommendationEngine.recommendFreelancers(
                jobPosting.getId(),
                jobPosting.getTitle(),
                jobPosting.getDescription(),
                AiRecommendationResponseDTO.class
        );
    }

    @Override     // 프리랜서용 추천
    public List<AiRecommendationResponseDTO> getRecommendedJobsForFreelancer(Long userId) {
        // 1. 프리랜서 정보 조회
        RecruitmentUser freelancer = recruitmentUserReader.getFreelancerByIdOrThrow(userId);

        // 2. 추천에 필요한 텍스트 가공
        String skills = (freelancer.skills() == null || freelancer.skills().isBlank())
                ? "없음" : freelancer.skills().trim();
        String experience = (freelancer.experience() == null || freelancer.experience().isBlank())
                ? "없음" : freelancer.experience().trim();

        // 3. AI 서버 호출
        return recommendationEngine.recommendJobs(
                userId,
                skills,
                experience,
                AiRecommendationResponseDTO.class
        );
    }

    @Transactional
    public void completeProject(Long projectId, Long userId) {
        Project project = projectPostingRepo.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        if (!project.getJobPosting().getEmployerId().equals(userId)) {
            throw new BusinessException(ErrorCode.JOB_POSTING_FORBIDDEN); // 권한 없음 에러
        }

        try{
            project.complete();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.PROJECT_ALREADY_COMPLETED);
        }

        Long freelancerId = project.getFreelancerId();
        RecruitmentUser freelancer = recruitmentUserReader.getFreelancerByIdOrThrow(freelancerId);
        String syncContent = String.format("프로젝트 완료: %s", project.getProjectName());

        Runnable syncTask = () -> {
            try {
                recommendationEngine.syncToAiServer(
                        freelancer.id(),
                        "experience",
                        syncContent,
                        freelancer.status()
                );
            } catch (Exception e) {
                log.error("프로젝트 완료 후 AI 서버 동기화 실패 - 프리랜서 ID: {}, 내용: {}", freelancer.id(), syncContent, e);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    syncTask.run();
                }
            });
        } else {
            log.warn("활성화된 트랜잭션이 없어 즉시 AI 동기화를 실행합니다. 프로젝트 ID: {}", projectId);
            syncTask.run();
        }

        runAfterCommit(() -> redisTemplate.delete(employerProjectsCacheKey(userId)));
    }

    private void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
            return;
        }
        task.run();
    }

    private String employerJobsCacheKey(Long employerId) {
        return CACHE_PREFIX + ":employer:jobs:" + employerId;
    }

    private String employerProjectsCacheKey(Long employerId) {
        return CACHE_PREFIX + ":employer:projects:" + employerId;
    }

    private String allJobsCacheKey() {
        return CACHE_PREFIX + ":jobs:all";
    }

    private String freelancerSearchCacheKey(Long freelancerId, String normalizedKeyword, boolean favoritesOnly) {
        return CACHE_PREFIX + ":freelancer:search:" + freelancerId + ":" + normalizedKeyword + ":" + favoritesOnly;
    }

    private void evictEmployerSideCaches(Long employerId) {
        redisTemplate.delete(employerJobsCacheKey(employerId));
        redisTemplate.delete(allJobsCacheKey());
        evictAllFreelancerSearchCaches();
    }

    private void evictFreelancerSearchCaches(Long freelancerId) {
        deleteByPattern(CACHE_PREFIX + ":freelancer:search:" + freelancerId + ":*");
    }

    private void evictAllFreelancerSearchCaches() {
        deleteByPattern(CACHE_PREFIX + ":freelancer:search:*");
    }

    private void deleteByPattern(String pattern) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(SCAN_BATCH_SIZE)
                    .build();

            List<byte[]> batch = new ArrayList<>(SCAN_BATCH_SIZE);
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= SCAN_BATCH_SIZE) {
                        connection.del(batch.toArray(new byte[0][]));
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    connection.del(batch.toArray(new byte[0][]));
                }
            } catch (Exception e) {
                log.warn("Failed to evict cache keys by pattern. pattern={}", pattern, e);
            }
            return null;
        });
    }

    private void writeCache(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value, CACHE_TTL);
        } catch (RuntimeException e) {
            log.warn("Failed to write cache. key={}", key, e);
        }
    }

    private <T> T readCache(String key, TypeReference<T> typeReference) {
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(key);
        } catch (RuntimeException e) {
            log.warn("Failed to read cache. key={}", key, e);
            return null;
        }
        if (cached == null) {
            return null;
        }
        try {
            return objectMapper.convertValue(cached, typeReference);
        } catch (Exception e) {
            log.warn("Failed to convert cache value. key={}", key, e);
            try {
                redisTemplate.delete(key);
            } catch (RuntimeException deleteException) {
                log.warn("Failed to delete invalid cache entry. key={}", key, deleteException);
            }
            return null;
        }
    }
}
