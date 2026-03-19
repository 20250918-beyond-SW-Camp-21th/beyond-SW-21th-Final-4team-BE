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
import com.fallguys.recruitment.api.dto.response.MatchedFreelancerResponseDTO;
import com.fallguys.recruitment.entity.JobPostingFavorite;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.entity.ProjectStatus;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.repository.JobPostingFavoriteRepo;
import com.fallguys.recruitment.repository.JobPostingRepo;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import com.fallguys.recruitment.service.port.RecruitmentUser;
import com.fallguys.recruitment.service.port.RecruitmentUserReader;
import com.fallguys.recruitment.service.support.RecommendationPendingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobPostingServiceImpl implements JobPostingService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final String CACHE_PREFIX = "recruitment";
    private static final int SCAN_BATCH_SIZE = 1000;
    private static final String EMPLOYER_PROJECT_STATS_KEY_PREFIX = "employer:project:stats:";
    private static final String EMPLOYER_PROJECT_LIST_KEY_PREFIX = "employer:project:list:";
    private static final String FREELANCER_PROJECT_STATS_KEY_PREFIX = "freelancer:project:stats:";
    private static final DateTimeFormatter ISO_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Duration AI_RECOMMENDATION_CACHE_TTL = Duration.ofHours(24);
    private static final Duration AI_RECOMMENDATION_LOCK_TTL = Duration.ofMinutes(10);
    private static final Duration AI_RECOMMENDATION_WAIT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration AI_RECOMMENDATION_WAIT_INTERVAL = Duration.ofMillis(200);
    private static final int AI_RECOMMENDATION_WARM_UP_LIMIT = 3;
    private static final String JOB_RECOMMENDATION_CACHE_KEY_PREFIX = "ai:reco:jobs:v3:";
    private static final String JOB_RECOMMENDATION_LOCK_KEY_PREFIX = "ai:lock:jobs:";

    private final JobPostingRepo jobPostingRepo;
    private final JobPostingFavoriteRepo jobPostingFavoriteRepo;
    private final ProjectPostingRepo projectPostingRepo;
    private final RecruitmentUserReader recruitmentUserReader;
    private final RecommendationEngine recommendationEngine; // AiAdapter 주입
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private JobPostingService self;

    @Override
    @Transactional(readOnly = true)
    public List<JobPostingSearchDTO> getJobPostings(Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        String cacheKey = employerJobsCacheKey(user.id());

        List<JobPostingSearchDTO> cached = readCache(cacheKey, new TypeReference<>() {});
        if (cached != null) {
            warmUpFreelancerRecommendationCaches(user.id(), cached, AI_RECOMMENDATION_WARM_UP_LIMIT);
            return cached;
        }

        List<JobPostingSearchDTO> loaded = jobPostingRepo.findAllByEmployerIdAndStatusNot(user.id(), Status.DELETED)
                .stream()
                .map(this::toJobPostingSearchDto)
                .toList();
        writeCache(cacheKey, loaded);
        warmUpFreelancerRecommendationCaches(user.id(), loaded, AI_RECOMMENDATION_WARM_UP_LIMIT);
        return loaded;
    }

    @Override
    @Transactional
    public void createJobPosting(JobPostingCreateDTO jobPostingCreateDTO, Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        JobPosting jobPosting = JobPosting.from(jobPostingCreateDTO, user.id(), user.name());
        jobPostingRepo.save(jobPosting);
        runAfterCommitSafely(() -> {
            syncJobPostingToAi(jobPosting);
            evictAllJobRecommendationCaches();
            evictEmployerSideCaches(user.id());
            refreshEmployerProjectStatsForMypage(user.id());
            refreshEmployerProjectListForMypage(user.id());
            self.triggerFreelancerRecommendation(jobPosting.getId(), user.id()); // Async trigger AI matching
        });
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
        runAfterCommitSafely(() -> {
            syncJobPostingToAi(jobPosting);
            evictAllJobRecommendationCaches();
            evictEmployerSideCaches(user.id());
            refreshEmployerProjectStatsForMypage(user.id());
            refreshEmployerProjectListForMypage(user.id());

            redisTemplate.delete("ai:reco:freelancers:" + jobPosting.getId());
            redisTemplate.delete("ai:lock:freelancers:" + jobPosting.getId());

            self.triggerFreelancerRecommendation(jobPosting.getId(), user.id()); // Re-trigger AI matching on update
        });
    }

    @Override
    @Transactional
    public void deleteJobPosting(Long jobPostingId, Long userId) {
        RecruitmentUser user = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateOwnership(jobPosting, user.id());
        validateNotDeleted(jobPosting);
        jobPosting.delete();
        runAfterCommitSafely(() -> {
            syncJobPostingToAi(jobPosting);
            evictAllJobRecommendationCaches();
            evictEmployerSideCaches(user.id());
            refreshEmployerProjectStatsForMypage(user.id());
            refreshEmployerProjectListForMypage(user.id());
        });
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
    public Page<MatchedFreelancerResponseDTO> getMatchedFreelancers(Long projectId, Long userId, Pageable pageable) {
        RecruitmentUser employer = recruitmentUserReader.getEmployerByIdOrThrow(userId);
        Project sourceProject = getProjectOrThrow(projectId);
        validateProjectOwnership(sourceProject, employer.id());

        Page<Project> projects = projectPostingRepo.findAllByJobPostingIdOrderByCreatedAtDesc(
                        sourceProject.getJobPosting().getId(),
                        pageable
                );

        Map<Long, RecruitmentUser> freelancersById = recruitmentUserReader.getFreelancersByIdsOrThrow(
                projects.getContent().stream()
                        .map(Project::getFreelancerId)
                        .filter(Objects::nonNull)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new))
        );

        return projects.map(project -> toMatchedFreelancerResponseDto(
                project,
                getFreelancerOrThrow(freelancersById, project.getFreelancerId())
        ));
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
        runAfterCommitSafely(() -> evictFreelancerSearchCaches(freelancerId));
    }

    @Override
    @Transactional
    public void removeFavoriteJobPosting(Long userId, Long jobPostingId) {
        RecruitmentUser user = recruitmentUserReader.getFreelancerByIdOrThrow(userId);
        Long freelancerId = user.id();

        jobPostingFavoriteRepo.deleteByFreelancerIdAndJobPostingId(freelancerId, jobPostingId);
        runAfterCommitSafely(() -> evictFreelancerSearchCaches(freelancerId));
    }

    private JobPosting getJobPostingOrThrow(Long jobPostingId) {
        return jobPostingRepo.findById(jobPostingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_POSTING_NOT_FOUND));
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectPostingRepo.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
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

    private void validateProjectOwnership(Project project, Long userId) {
        if (!project.getEmployerId().equals(userId)) {
            throw new BusinessException(ErrorCode.JOB_POSTING_FORBIDDEN);
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

    private MatchedFreelancerResponseDTO toMatchedFreelancerResponseDto(Project project, RecruitmentUser freelancer) {
        return new MatchedFreelancerResponseDTO(
                project.getId(),
                project.getFreelancerId(),
                freelancer.name(),
                freelancer.skills(),
                freelancer.experience(),
                freelancer.status(),
                project.getStatus(),
                project.getCreatedAt()
        );
    }

    private RecruitmentUser getFreelancerOrThrow(Map<Long, RecruitmentUser> freelancersById, Long freelancerId) {
        RecruitmentUser freelancer = freelancersById.get(freelancerId);
        if (freelancer == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return freelancer;
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

    private String joinJobTechStack(List<String> techStack) {
        return orEmpty(techStack).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String buildJobPostingAiContent(JobPosting jobPosting) {
        String techStack = joinJobTechStack(jobPosting.getTechStack());

        StringBuilder builder = new StringBuilder();
        builder.append("Title: ").append(Optional.ofNullable(jobPosting.getTitle()).orElse("")).append('\n');
        builder.append("Description: ").append(Optional.ofNullable(jobPosting.getDescription()).orElse("")).append('\n');
        builder.append("Tech Stack: ").append(techStack).append('\n');
        builder.append("Budget: ").append(Optional.ofNullable(jobPosting.getBudget()).orElse(0L)).append('\n');
        builder.append("Duration: ").append(Optional.ofNullable(jobPosting.getDuration()).orElse(0)).append('\n');
        builder.append("Employer: ").append(Optional.ofNullable(jobPosting.getEmployerName()).orElse("")).append('\n');
        builder.append("Headcount: ").append(Optional.ofNullable(jobPosting.getHeadcount()).orElse(0)).append('\n');
        builder.append("Matched Headcount: ").append(Optional.ofNullable(jobPosting.getMatchedHeadcount()).orElse(0)).append('\n');
        builder.append("Posting Status: ").append(
                jobPosting.getPostingStatus() == null ? "" : jobPosting.getPostingStatus().name()
        );
        return builder.toString();
    }

    private String resolveJobPostingAiStatus(JobPosting jobPosting) {
        if (jobPosting.getStatus() != Status.ACTIVE) {
            return jobPosting.getStatus().name();
        }

        JobPostingStatus postingStatus = jobPosting.getPostingStatus();
        if (postingStatus == null) {
            return Status.ACTIVE.name();
        }

        if (!EnumSet.of(JobPostingStatus.OPEN, JobPostingStatus.IN_PROGRESS).contains(postingStatus)) {
            return postingStatus.name();
        }

        return Status.ACTIVE.name();
    }

    private void syncJobPostingToAi(JobPosting jobPosting) {
        recommendationEngine.syncToAiServer(
                jobPosting.getId(),
                jobPosting.getId(),
                "job_posting",
                buildJobPostingAiContent(jobPosting),
                resolveJobPostingAiStatus(jobPosting)
        );
    }

    private String normalizeRecommendationSearchText(String value) {
        if (value == null) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.isBlank() || "\uC5C6\uC74C".equals(trimmed)) {
            return "";
        }

        return trimmed;
    }

    private boolean hasFreelancerSkillOverlap(List<String> jobTechStack, String freelancerSkills) {
        java.util.Set<String> requiredSkills = orEmpty(jobTechStack).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .map(skill -> skill.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        if (requiredSkills.isEmpty()) {
            return true;
        }

        java.util.Set<String> candidateSkills = java.util.Arrays.stream(
                        Optional.ofNullable(freelancerSkills).orElse("").split(",")
                )
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .map(skill -> skill.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());

        if (candidateSkills.isEmpty()) {
            return false;
        }

        return requiredSkills.stream().anyMatch(candidateSkills::contains);
    }

    @Override   // 기업용: 캐싱 조회 전용
    public List<AiRecommendationResponseDTO> getRecommendedFreelancers(Long jobPostingId, Long userId) {
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateNotDeleted(jobPosting);
        validateOwnership(jobPosting, userId);

        String cacheKey = "ai:reco:freelancers:" + jobPostingId;
        String lockKey = "ai:lock:freelancers:" + jobPostingId;
        List<AiRecommendationResponseDTO> cached = readCache(cacheKey, new TypeReference<>() {});
        if (cached != null) {
            log.info("Freelancer recommendation cache hit. jobPostingId={}, result={}", jobPostingId, summarizeRecommendationIds(cached));
            return cached;
        }

        log.info(
                "Freelancer recommendation requested. jobPostingId={}, employerId={}, techStackCount={}",
                jobPostingId,
                userId,
                jobPosting.getTechStack() == null ? 0 : jobPosting.getTechStack().size()
        );
        log.debug(
                "Freelancer recommendation request detail. jobPostingId={}, employerId={}, title={}, techStack={}",
                jobPostingId,
                userId,
                maskSensitive(jobPosting.getTitle()),
                maskSensitive(String.valueOf(jobPosting.getTechStack()))
        );

        generateFreelancerRecommendation(jobPostingId, userId);

        List<AiRecommendationResponseDTO> refreshed = readCache(cacheKey, new TypeReference<>() {});
        if (refreshed != null) {
            return refreshed;
        }

        List<AiRecommendationResponseDTO> warmedUp = awaitRecommendationCache(
                cacheKey,
                new TypeReference<>() {},
                AI_RECOMMENDATION_WAIT_TIMEOUT
        );
        if (warmedUp != null) {
            return warmedUp;
        }

        if (isRecommendationPending(lockKey)) {
            throw new RecommendationPendingException("Freelancer recommendation is still being generated.");
        }

        return java.util.Collections.emptyList();
    }

    @org.springframework.scheduling.annotation.Async
    @Override
    public void triggerFreelancerRecommendation(Long jobPostingId, Long userId) {
        generateFreelancerRecommendation(jobPostingId, userId);
    }

    private void generateFreelancerRecommendation(Long jobPostingId, Long userId) {
        String lockKey = "ai:lock:freelancers:" + jobPostingId;
        String lockToken = java.util.UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, AI_RECOMMENDATION_LOCK_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            log.info("AI 추천 처리 중복 트리거 방지 (Job ID: {})", jobPostingId);
            return; // 이미 10분 내에 누군가 동작시켰다면 무시
        }

        try {
            JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
            if (jobPosting.getStatus() != Status.ACTIVE
                    || !EnumSet.of(JobPostingStatus.OPEN, JobPostingStatus.IN_PROGRESS)
                    .contains(jobPosting.getPostingStatus())) {
                log.info(
                        "Freelancer recommendation skipped. jobPostingId={}, employerId={}, status={}, postingStatus={}",
                        jobPostingId,
                        userId,
                        jobPosting.getStatus(),
                        jobPosting.getPostingStatus()
                );
                return;
            }

            List<AiRecommendationResponseDTO> aiResults = recommendationEngine.recommendFreelancers(
                    jobPosting.getId(),
                    jobPosting.getTitle(),
                    jobPosting.getDescription(),
                    joinJobTechStack(jobPosting.getTechStack()),
                    AiRecommendationResponseDTO.class
            );

            log.info(
                    "Freelancer recommendation AI raw result. jobPostingId={}, employerId={}, resultCount={}, result={}",
                    jobPostingId,
                    userId,
                    aiResults == null ? 0 : aiResults.size(),
                    summarizeRecommendationIds(aiResults)
            );
            log.debug(
                    "Freelancer recommendation AI request detail. jobPostingId={}, employerId={}, title={}",
                    jobPostingId,
                    userId,
                    maskSensitive(jobPosting.getTitle())
            );

            List<AiRecommendationResponseDTO> safeAiResults = orEmpty(aiResults);
            List<Long> freelancerIds = safeAiResults.stream()
                    .map(AiRecommendationResponseDTO::id)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .toList();
            log.info(
                    "Freelancer recommendation candidate summary. jobPostingId={}, employerId={}, candidateCount={}, candidateIds={}",
                    jobPostingId,
                    userId,
                    freelancerIds.size(),
                    freelancerIds
            );
            Map<Long, RecruitmentUser> userMap = loadFreelancersByFreelancerIds(freelancerIds);
            try {
                userMap = recruitmentUserReader.getFreelancersByFreelancerIdsOrThrow(freelancerIds);
            } catch (Exception e) {
                log.warn("AI 추천 결과 보정 실패 - 프리랜서 일괄 조회 실패", e);
                userMap = loadFreelancersByFreelancerIds(freelancerIds);
            }

            Map<Long, RecruitmentUser> combinedUserMap = new LinkedHashMap<>(userMap);
            log.info(
                    "Freelancer recommendation user lookup summary. jobPostingId={}, employerId={}, requestedCount={}, resolvedCount={}",
                    jobPostingId,
                    userId,
                    freelancerIds.size(),
                    userMap.size()
            );

            List<Long> missingIds = safeAiResults.stream()
                    .map(AiRecommendationResponseDTO::id)
                    .filter(java.util.Objects::nonNull)
                    .filter(id -> !combinedUserMap.containsKey(id))
                    .distinct()
                    .toList();
            if (!missingIds.isEmpty()) {
                log.warn(
                        "Freelancer recommendation unresolved candidates after bulk lookup. jobPostingId={}, employerId={}, missingCount={}, missingIds={}",
                        jobPostingId,
                        userId,
                        missingIds.size(),
                        missingIds
                );
            }
            if (!missingIds.isEmpty()) {
                try {
                    combinedUserMap.putAll(recruitmentUserReader.getFreelancersByFreelancerIdsOrThrow(missingIds));
                } catch (Exception e) {
                    log.warn("AI 추천 결과 보정 실패 - 누락 프리랜서 일괄 조회 실패", e);
                    for (Long missingId : missingIds) {
                        try {
                            combinedUserMap.put(missingId, recruitmentUserReader.getFreelancerByFreelancerIdOrThrow(missingId));
                        } catch (Exception singleFetchException) {
                            log.warn("AI 추천 결과 보정 실패 - 프리랜서 개별 조회 실패. freelancerId={}", missingId, singleFetchException);
                        }
                    }
                }
            }
            log.info(
                    "Freelancer recommendation final lookup summary. jobPostingId={}, employerId={}, resolvedCount={}, unresolvedCount={}",
                    jobPostingId,
                    userId,
                    combinedUserMap.size(),
                    Math.max(0, freelancerIds.size() - combinedUserMap.size())
            );

            final Map<Long, RecruitmentUser> finalUserMap = combinedUserMap;
            List<AiRecommendationResponseDTO> result = safeAiResults.stream().map(dto -> {
                try {
                    RecruitmentUser f = finalUserMap.get(dto.id());
                    if (f == null) {
                        log.warn(
                                "Freelancer recommendation candidate dropped. jobPostingId={}, employerId={}, freelancerId={}",
                                jobPostingId,
                                userId,
                                dto.id()
                        );
                        return null;
                    }
                    List<String> userSkills = (f.skills() != null && !f.skills().trim().isEmpty())
                            ? java.util.Arrays.asList(f.skills().split(",")) 
                            : java.util.Collections.emptyList();
                    if (!hasFreelancerSkillOverlap(jobPosting.getTechStack(), f.skills())) {
                        log.info(
                                "Freelancer recommendation candidate filtered by skill overlap. jobPostingId={}, employerId={}, freelancerId={}",
                                jobPostingId,
                                userId,
                                dto.id()
                        );
                        return null;
                    }
                    return dto.withFreelancerInfo(f.name(), userSkills, f.experience());
                } catch (Exception e) {
                    log.warn(
                            "Freelancer recommendation enrichment failed. jobPostingId={}, employerId={}, freelancerId={}",
                            jobPostingId,
                            userId,
                            dto.id(),
                            e
                    );
                    return null;
                }
            }).filter(Objects::nonNull).toList();

            log.info(
                    "Freelancer recommendation enriched result. jobPostingId={}, employerId={}, resultCount={}, result={}",
                    jobPostingId,
                    userId,
                    result.size(),
                    summarizeRecommendationIds(result)
            );
            if (safeAiResults.isEmpty()) {
                log.warn(
                        "Freelancer recommendation empty result reason. jobPostingId={}, employerId={}, reason=ai_returned_no_candidates",
                        jobPostingId,
                        userId
                );
            } else if (result.isEmpty()) {
                log.warn(
                        "Freelancer recommendation empty result reason. jobPostingId={}, employerId={}, reason=enrichment_lookup_or_skill_overlap_removed_all_candidates, candidateCount={}, resolvedCount={}",
                        jobPostingId,
                        userId,
                        freelancerIds.size(),
                        combinedUserMap.size()
                );
            }

            String cacheKey = "ai:reco:freelancers:" + jobPostingId;
            writeCacheWithTtl(cacheKey, result, AI_RECOMMENDATION_CACHE_TTL);
        } catch (Exception e) {
            log.error("Async Freelancer Recommendation failed for Job: {}", jobPostingId, e);
        } finally {
            releaseLockIfOwned(lockKey, lockToken);
        }
    }

    @Override     // 프리랜서용: 캐싱 조회 전용
    public List<AiRecommendationResponseDTO> getRecommendedJobsForFreelancer(Long userId) {
        // Validation check to prevent triggering background task for bad userIds
        RecruitmentUser freelancer = recruitmentUserReader.getFreelancerByIdOrThrow(userId);

        String cacheKey = jobRecommendationCacheKey(userId);
        String lockKey = jobRecommendationLockKey(userId);
        List<AiRecommendationResponseDTO> cached = readCache(cacheKey, new TypeReference<>() {});
        if (cached != null) {
            log.info("Job recommendation cache hit. userId={}, result={}", userId, summarizeRecommendationIds(cached));
            return cached;
        }

        log.info("Job recommendation requested. userId={}", userId);
        log.debug(
                "Job recommendation request detail. userId={}, skills={}, experience={}",
                userId,
                maskSensitive(freelancer.skills()),
                maskSensitive(freelancer.experience())
        );

        self.triggerJobRecommendation(userId);

        List<AiRecommendationResponseDTO> warmedUp = awaitRecommendationCache(
                cacheKey,
                new TypeReference<>() {},
                AI_RECOMMENDATION_WAIT_TIMEOUT
        );
        if (warmedUp != null) {
            return warmedUp;
        }

        if (isRecommendationPending(lockKey)) {
            throw new RecommendationPendingException("Job recommendation is still being generated.");
        }

        return java.util.Collections.emptyList();
    }

    @org.springframework.scheduling.annotation.Async
    @Override
    public void triggerJobRecommendation(Long userId) {
        String lockKey = jobRecommendationLockKey(userId);
        String lockToken = java.util.UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, AI_RECOMMENDATION_LOCK_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            log.info("AI 추천 처리 중복 트리거 방지 (User ID: {})", userId);
            return;
        }

        try {
            RecruitmentUser freelancer = recruitmentUserReader.getFreelancerByIdOrThrow(userId);
            String rawSkills = (freelancer.skills() == null || freelancer.skills().isBlank())
                    ? "없음" : freelancer.skills().trim();
            String rawExperience = (freelancer.experience() == null || freelancer.experience().isBlank())
                    ? "없음" : freelancer.experience().trim();
            final String skills = normalizeRecommendationSearchText(rawSkills);
            final String experience = normalizeRecommendationSearchText(rawExperience);

            log.debug(
                    "Job recommendation AI request detail. userId={}, rawSkills={}, rawExperience={}",
                    userId,
                    maskSensitive(skills),
                    maskSensitive(experience)
            );

            List<AiRecommendationResponseDTO> aiResults = recommendationEngine.recommendJobs(
                    userId,
                    skills,
                    experience,
                    AiRecommendationResponseDTO.class
            );

            log.info(
                    "Job recommendation AI raw result. userId={}, result={}",
                    userId,
                    summarizeRecommendationIds(aiResults)
            );

            List<Long> jobIds = aiResults.stream().map(AiRecommendationResponseDTO::id).toList();
            Map<Long, JobPosting> jobsMap = jobPostingRepo.findAllById(jobIds).stream()
                    .collect(java.util.stream.Collectors.toMap(JobPosting::getId, j -> j));

            List<AiRecommendationResponseDTO> result = aiResults.stream().map(dto -> {
                try {
                    JobPosting job = jobsMap.get(dto.id());
                    if (job == null || job.getStatus() != Status.ACTIVE) {
                        return null;
                    }
                    if (!EnumSet.of(JobPostingStatus.OPEN, JobPostingStatus.IN_PROGRESS).contains(job.getPostingStatus())) {
                        return null;
                    }
                    if (!hasRecommendedJobSkillOverlap(job.getTechStack(), skills)) {
                        log.info(
                                "Dropping job recommendation without tech stack overlap. userId={}, jobId={}",
                                userId,
                                job.getId()
                        );
                        return null;
                    }
                    return dto.withJobInfo(job.getTechStack(), job.getDescription(), job.getBudget(), job.getDuration());
                } catch (Exception e) {
                    return null;
                }
            }).filter(Objects::nonNull).toList();

            log.info(
                    "Job recommendation filtered result. userId={}, result={}",
                    userId,
                    summarizeRecommendationIds(result)
            );

            String cacheKey = jobRecommendationCacheKey(userId);
            writeCacheWithTtl(cacheKey, result, AI_RECOMMENDATION_CACHE_TTL);
        } catch (Exception e) {
            log.error("Async Job Recommendation failed for Freelancer: {}", userId, e);
        } finally {
            releaseLockIfOwned(lockKey, lockToken);
        }
    }

    private void warmUpFreelancerRecommendationCaches(Long employerId, List<JobPostingSearchDTO> jobPostings, int maxWarmUps) {
        int triggered = 0;
        for (JobPostingSearchDTO jobPosting : orEmpty(jobPostings)) {
            if (triggered >= maxWarmUps) {
                break;
            }

            if (jobPosting == null || !EnumSet.of(JobPostingStatus.OPEN, JobPostingStatus.IN_PROGRESS).contains(jobPosting.status())) {
                continue;
            }

            String cacheKey = "ai:reco:freelancers:" + jobPosting.jobPostingId();
            String lockKey = "ai:lock:freelancers:" + jobPosting.jobPostingId();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey)) || Boolean.TRUE.equals(redisTemplate.hasKey(lockKey))) {
                continue;
            }

            self.triggerFreelancerRecommendation(jobPosting.jobPostingId(), employerId);
            triggered++;
        }
    }

    private <T> T awaitRecommendationCache(String key, TypeReference<T> typeReference, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            T cached = readCache(key, typeReference);
            if (cached != null) {
                return cached;
            }

            try {
                Thread.sleep(AI_RECOMMENDATION_WAIT_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return readCache(key, typeReference);
    }

    private void releaseLockIfOwned(String lockKey, String lockToken) {
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                    java.util.Collections.singletonList(lockKey),
                    lockToken
            );
        } catch (Exception ignored) {
        }
    }

    private boolean isRecommendationPending(String lockKey) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.warn("Failed to read recommendation lock state. key={}", lockKey, e);
            return false;
        }
    }

    private void writeCacheWithTtl(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("레디스 쓰기 실패: {}", key, e);
        }
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
        RecruitmentUser freelancer = recruitmentUserReader.getFreelancerByFreelancerIdOrThrow(freelancerId);
        String syncContent = String.format("프로젝트 완료: %s", project.getProjectName());

        Runnable syncTask = () -> {
            try {
                recommendationEngine.syncProjectExperience(
                        projectId,
                        freelancerId,
                        syncContent,
                        "COMPLETED"
                );
            } catch (Exception e) {
                log.error("프로젝트 완료 후 AI 서버 동기화 실패 - 프리랜서 ID: {}, 내용: {}", freelancerId, syncContent, e);
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

        runAfterCommitSafely(() -> {
            redisTemplate.delete(employerProjectsCacheKey(userId));
            refreshEmployerProjectStatsForMypage(userId);
            refreshFreelancerProjectStatsForMypage(freelancerId);
        });
    }

    @Override
    @Transactional
    public void closeJobPosting(Long jobPostingId) {
        JobPosting jobPosting = getJobPostingOrThrow(jobPostingId);
        validateNotDeleted(jobPosting);

        jobPosting.closeRecruitment();

        runAfterCommitSafely(() -> {
            syncJobPostingToAi(jobPosting);
            evictAllJobRecommendationCaches();
            Long employerId = jobPosting.getEmployerId();
            evictEmployerSideCaches(employerId);
            redisTemplate.delete(employerProjectsCacheKey(employerId));
            refreshEmployerProjectStatsForMypage(employerId);
            refreshEmployerProjectListForMypage(employerId);
        });
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

    private void runAfterCommitSafely(Runnable task) {
        runAfterCommit(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                log.warn("Failed to execute post-commit task", e);
            }
        });
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

    private void refreshEmployerProjectStatsForMypage(Long employerId) {
        refreshEmployerProjectStatsCache(employerId);
    }

    @Override
    public void refreshEmployerProjectStatsCache(Long employerId) {
        if (employerId == null) {
            return;
        }

        List<JobPosting> postings = orEmpty(jobPostingRepo.findAllByEmployerIdAndStatusNot(employerId, Status.DELETED));
        List<Project> projects = orEmpty(projectPostingRepo.findAllByEmployerIdOrderByCreatedAtDesc(employerId));

        int activeApplicants = (int) projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.IN_PROGRESS)
                .count();
        int contractedFreelancers = (int) projects.stream()
                .map(Project::getFreelancerId)
                .filter(id -> id != null)
                .distinct()
                .count();

        Map<String, Object> payload = new HashMap<>();
        payload.put("totalProjects", postings.size());
        payload.put("activeApplicants", activeApplicants);
        payload.put("contractedFreelancers", contractedFreelancers);

        writeMypageRedisValue(EMPLOYER_PROJECT_STATS_KEY_PREFIX + employerId, payload);
    }

    @Override
    public void refreshEmployerRecruitmentCaches(Long employerId) {
        if (employerId == null) {
            return;
        }

        redisTemplate.delete(employerJobsCacheKey(employerId));
        redisTemplate.delete(employerProjectsCacheKey(employerId));
        redisTemplate.delete(allJobsCacheKey());
    }

    private void refreshEmployerProjectListForMypage(Long employerId) {
        String redisKey = EMPLOYER_PROJECT_LIST_KEY_PREFIX + employerId;
        Map<Long, Integer> cachedApplicantCounts = readCachedEmployerApplicantCounts(redisKey);

        List<Map<String, Object>> payload = orEmpty(jobPostingRepo.findAllByEmployerIdAndStatusNot(employerId, Status.DELETED))
                .stream()
                .map(posting -> toEmployerProjectListItem(posting, cachedApplicantCounts.get(posting.getId())))
                .sorted(Comparator.comparing(
                        this::extractCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .toList();
        writeMypageRedisValue(redisKey, payload);
    }

    private Map<Long, Integer> readCachedEmployerApplicantCounts(String redisKey) {
        Map<Long, Integer> applicantCounts = new HashMap<>();
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (!(cached instanceof List<?> cachedList)) {
                return applicantCounts;
            }

            for (Object entry : cachedList) {
                if (!(entry instanceof Map<?, ?> cachedItem)) {
                    continue;
                }
                Long projectId = parseLongSafely(cachedItem.get("projectId"));
                Integer applicantCount = parseIntegerSafely(cachedItem.get("applicantCount"));
                if (projectId == null || applicantCount == null) {
                    continue;
                }
                applicantCounts.put(projectId, applicantCount);
            }
        } catch (RuntimeException e) {
            log.warn("Failed to read employer project list cache for applicantCount reuse. key={}", redisKey, e);
        }
        return applicantCounts;
    }

    private Map<String, Object> toEmployerProjectListItem(JobPosting posting, Integer cachedApplicantCount) {
        Map<String, Object> item = new HashMap<>();
        item.put("projectId", posting.getId());
        item.put("title", posting.getTitle());
        item.put("status", toEmployerProjectStatus(posting.getPostingStatus()));
        item.put("applicantCount", cachedApplicantCount != null ? cachedApplicantCount : posting.getMatchedHeadcount());
        item.put("description", posting.getDescription());
        item.put("monthlySalary", posting.getBudget());

        LocalDateTime createdAt = posting.getCreatedAt();
        item.put("createdAt", createdAt != null ? createdAt.format(ISO_SECONDS_FORMATTER) : null);

        // Job posting duration is an estimated project period in months, not a recruitment deadline.
        item.put("deadline", null);
        return item;
    }

    private LocalDateTime extractCreatedAt(Map<String, Object> item) {
        if (item == null) {
            return null;
        }
        Object createdAt = item.get("createdAt");
        if (createdAt == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(createdAt.toString(), ISO_SECONDS_FORMATTER);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Long parseLongSafely(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseIntegerSafely(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String toEmployerProjectStatus(JobPostingStatus status) {
        if (status == null) {
            return "모집중";
        }
        return switch (status) {
            case OPEN -> "모집중";
            case IN_PROGRESS -> "진행중";
            case COMPLETED -> "완료";
            case CLOSED -> "마감";
        };
    }

    private void refreshFreelancerProjectStatsForMypage(Long freelancerId) {
        List<Project> projects = orEmpty(projectPostingRepo.findAllByFreelancerIdOrderByCreatedAtDesc(freelancerId));
        int inProgressProjects = (int) projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.IN_PROGRESS)
                .count();
        int completedProjects = (int) projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.COMPLETED)
                .count();

        String redisKey = FREELANCER_PROJECT_STATS_KEY_PREFIX + freelancerId;
        Map<String, Object> payload = readFreelancerProjectStatsPayload(redisKey);
        Integer appliedProjects = readAppliedProjectsCount(payload);
        if (appliedProjects != null) {
            payload.put("appliedProjects", appliedProjects);
        }

        payload.put("inProgressProjects", inProgressProjects);
        payload.put("completedProjects", completedProjects);
        writeMypageRedisValue(redisKey, payload);
    }

    private Map<String, Object> readFreelancerProjectStatsPayload(String redisKey) {
        Map<String, Object> payload = new HashMap<>();
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached instanceof Map<?, ?> map) {
                map.forEach((key, value) -> payload.put(String.valueOf(key), value));
            }
        } catch (RuntimeException e) {
            log.warn("Failed to read freelancer project stats from mypage redis key. key={}", redisKey, e);
        }
        return payload;
    }

    private Integer readAppliedProjectsCount(Map<String, Object> payload) {
        if (payload == null || !payload.containsKey("appliedProjects")) {
            return null;
        }
        return parseIntegerSafely(payload.get("appliedProjects"));
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

    private boolean hasRecommendedJobSkillOverlap(List<String> jobTechStack, String freelancerSkills) {
        java.util.Set<String> requiredSkills = orEmpty(jobTechStack).stream()
                .filter(Objects::nonNull)
                .map(this::normalizeRecommendationSkillToken)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (requiredSkills.isEmpty()) {
            return false;
        }

        java.util.Set<String> candidateSkills = java.util.Arrays.stream(
                        Optional.ofNullable(freelancerSkills).orElse("").split(",")
                )
                .map(this::normalizeRecommendationSkillToken)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (candidateSkills.isEmpty()) {
            return false;
        }

        return requiredSkills.stream().anyMatch(candidateSkills::contains);
    }

    private String normalizeRecommendationSkillToken(String skill) {
        if (skill == null) {
            return null;
        }

        String normalized = skill.trim()
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("'", "");

        if (normalized.isBlank()) {
            return null;
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private void evictAllJobRecommendationCaches() {
        deleteByPattern(JOB_RECOMMENDATION_CACHE_KEY_PREFIX + "*");
    }

    private String jobRecommendationCacheKey(Long userId) {
        return JOB_RECOMMENDATION_CACHE_KEY_PREFIX + userId;
    }

    private String jobRecommendationLockKey(Long userId) {
        return JOB_RECOMMENDATION_LOCK_KEY_PREFIX + userId;
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

    private void writeMypageRedisValue(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (RuntimeException e) {
            log.warn("Failed to write mypage redis payload. key={}", key, e);
        }
    }

    private String summarizeRecommendationIds(List<AiRecommendationResponseDTO> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "[]";
        }

        return recommendations.stream()
                .filter(Objects::nonNull)
                .map(item -> "%s(%.2f)".formatted(
                        String.valueOf(item.id()),
                        item.matchScore() == null ? 0.0 : item.matchScore()
                ))
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
    }

    private String maskSensitive(String value) {
        if (value == null) {
            return "null";
        }

        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return normalized;
        }

        int minMask = 2;
        int length = normalized.length();
        int visiblePrefix = Math.min(2, Math.max(0, length - minMask));
        int remainingAfterPrefix = length - visiblePrefix;
        int visibleSuffix = remainingAfterPrefix > minMask
                ? Math.min(2, remainingAfterPrefix - minMask)
                : 0;
        int maskedLength = Math.max(minMask, length - visiblePrefix - visibleSuffix);

        String masked = normalized.substring(0, visiblePrefix)
                + "*".repeat(Math.min(maskedLength, 8))
                + (visibleSuffix > 0 ? normalized.substring(normalized.length() - visibleSuffix) : "");

        return masked.length() > 24 ? masked.substring(0, 24) + "..." : masked;
    }

    private Map<Long, RecruitmentUser> loadFreelancersByFreelancerIds(Collection<Long> freelancerIds) {
        if (freelancerIds == null || freelancerIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, RecruitmentUser> resolved = new LinkedHashMap<>();
        for (Long freelancerId : freelancerIds) {
            if (freelancerId == null || resolved.containsKey(freelancerId)) {
                continue;
            }
            try {
                resolved.put(
                        freelancerId,
                        recruitmentUserReader.getFreelancerByFreelancerIdOrThrow(freelancerId)
                );
            } catch (Exception e) {
                log.warn("Freelancer recommendation lookup failed. freelancerId={}", freelancerId, e);
            }
        }
        return resolved;
    }

    private <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
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
