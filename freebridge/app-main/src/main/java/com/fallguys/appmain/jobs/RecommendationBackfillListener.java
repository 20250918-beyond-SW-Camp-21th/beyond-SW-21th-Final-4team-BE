package com.fallguys.appmain.jobs;

import com.fallguys.common.event.RecommendationBackfillRequestedEvent;
import com.fallguys.mypage.entity.freelancer.Collaboration;
import com.fallguys.mypage.entity.freelancer.Expertise;
import com.fallguys.mypage.entity.freelancer.Freelancer;
import com.fallguys.mypage.entity.freelancer.WorkConditions;
import com.fallguys.mypage.repository.freelancer.FreelancerRepository;
import com.fallguys.recruitment.entity.JobPosting;
import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.repository.JobPostingRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationBackfillListener {

    private static final int SCAN_BATCH_SIZE = 1000;
    private static final String JOB_RECOMMENDATION_CACHE_KEY_PREFIX = "ai:reco:jobs:v2:";
    private static final String BACKFILL_DONE_KEY = "ai:backfill:recommendation:done";
    private static final String BACKFILL_LOCK_KEY = "ai:backfill:recommendation:lock";
    private static final Duration BACKFILL_LOCK_TTL = Duration.ofMinutes(30);

    private final JobPostingRepo jobPostingRepo;
    private final FreelancerRepository freelancerRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${fallguys.ai.python-url}")
    private String pythonUrl;

    @Async
    @EventListener
    public void handle(RecommendationBackfillRequestedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(BACKFILL_DONE_KEY))) {
            return;
        }

        String lockToken = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(BACKFILL_LOCK_KEY, lockToken, BACKFILL_LOCK_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            return;
        }

        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(BACKFILL_DONE_KEY))) {
                return;
            }

            RestClient restClient = buildRestClient();

            int jobSuccess = 0;
            int jobFailed = 0;
            int freelancerSuccess = 0;
            int freelancerFailed = 0;

            for (JobPosting jobPosting : jobPostingRepo.findAll()) {
                try {
                    syncData(
                            restClient,
                            jobPosting.getId(),
                            jobPosting.getId(),
                            "job_posting",
                            buildJobPostingAiContent(jobPosting),
                            resolveJobPostingAiStatus(jobPosting)
                    );
                    jobSuccess++;
                } catch (Exception e) {
                    jobFailed++;
                    log.error("Recommendation login backfill failed for job_posting. jobPostingId={}", jobPosting.getId(), e);
                }
            }

            for (Freelancer freelancer : freelancerRepository.findAll()) {
                try {
                    syncData(
                            restClient,
                            freelancer.getFreelancerId(),
                            freelancer.getFreelancerId(),
                            "new_profile",
                            buildFreelancerProfileAiContent(freelancer),
                            Optional.ofNullable(freelancer.getStatus()).map(Enum::name).orElse("POTENTIAL")
                    );
                    freelancerSuccess++;
                } catch (Exception e) {
                    freelancerFailed++;
                    log.error("Recommendation login backfill failed for new_profile. freelancerId={}", freelancer.getFreelancerId(), e);
                }
            }

            evictRecommendationCaches();
            redisTemplate.opsForValue().set(
                    BACKFILL_DONE_KEY,
                    "triggeredByUserId=%s,jobSuccess=%s,jobFailed=%s,freelancerSuccess=%s,freelancerFailed=%s".formatted(
                            event.userId(),
                            jobSuccess,
                            jobFailed,
                            freelancerSuccess,
                            freelancerFailed
                    )
            );

            log.info(
                    "Recommendation login backfill done. triggeredByUserId={}, jobSuccess={}, jobFailed={}, freelancerSuccess={}, freelancerFailed={}",
                    event.userId(),
                    jobSuccess,
                    jobFailed,
                    freelancerSuccess,
                    freelancerFailed
            );
        } catch (Exception e) {
            log.error("Recommendation login backfill failed before completion. triggeredByUserId={}", event.userId(), e);
        } finally {
            releaseLockIfOwned(BACKFILL_LOCK_KEY, lockToken);
        }
    }

    private RestClient buildRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(90).toMillis());
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private void syncData(RestClient restClient, Long id, Long refId, String type, String content, String status) {
        restClient.post()
                .uri(pythonUrl + "/api/v1/sync/data")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "id", id,
                        "refId", refId,
                        "type", type,
                        "content", content,
                        "status", status
                ))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Recommendation login backfill sync failed. status=" + response.getStatusCode());
                })
                .toBodilessEntity();
    }

    private String buildJobPostingAiContent(JobPosting jobPosting) {
        String techStack = Optional.ofNullable(jobPosting.getTechStack())
                .orElse(List.of())
                .stream()
                .filter(skill -> skill != null && !skill.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.joining(", "));

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

    private String buildFreelancerProfileAiContent(Freelancer freelancer) {
        StringBuilder builder = new StringBuilder();
        builder.append("Job: ").append(Optional.ofNullable(freelancer.getJob()).orElse("")).append('\n');
        builder.append("Introduction: ").append(Optional.ofNullable(freelancer.getIntroduction()).orElse("")).append('\n');
        builder.append("Skills: ").append(String.join(", ", Optional.ofNullable(freelancer.getSkills()).orElseGet(List::of))).append('\n');
        builder.append("Career Years: ").append(Optional.ofNullable(freelancer.getCareerYears()).orElse(0)).append('\n');
        builder.append("Wage: ").append(Optional.ofNullable(freelancer.getWage()).orElse(0L)).append('\n');
        builder.append("Grade: ").append(Optional.ofNullable(freelancer.getGrade()).map(Enum::name).orElse("")).append('\n');
        builder.append("Status: ").append(Optional.ofNullable(freelancer.getStatus()).map(Enum::name).orElse("POTENTIAL")).append('\n');
        builder.append("Expertise Average Rate: ").append(
                Optional.ofNullable(calculateExpertiseAverage(freelancer.getExpertise())).map(Object::toString).orElse("")
        ).append('\n');
        builder.append("Collaboration Average Rate: ").append(
                Optional.ofNullable(calculateCollaborationAverage(freelancer.getCollaboration())).map(Object::toString).orElse("")
        ).append('\n');
        builder.append("Average Rate: ").append(
                Optional.ofNullable(freelancer.getAverageRate()).map(Object::toString).orElse("")
        ).append('\n');

        WorkConditions workConditions = freelancer.getWorkConditions();
        if (workConditions != null) {
            builder.append("Work Type: ").append(Optional.ofNullable(workConditions.getConditionsType()).orElse("")).append('\n');
            builder.append("Available Start Date: ").append(Optional.ofNullable(workConditions.getStartDate()).map(Object::toString).orElse("")).append('\n');
            builder.append("Work Style: ").append(Optional.ofNullable(workConditions.getWorkStyle()).orElse("")).append('\n');
            builder.append("Work Location: ").append(Optional.ofNullable(workConditions.getLocation()).orElse("")).append('\n');
        }

        return builder.toString();
    }

    private Double calculateExpertiseAverage(Expertise expertise) {
        if (expertise == null) {
            return null;
        }

        int count = 0;
        double total = 0.0;

        if (expertise.getProgramming() != null) {
            total += expertise.getProgramming();
            count++;
        }
        if (expertise.getFramework() != null) {
            total += expertise.getFramework();
            count++;
        }
        if (expertise.getProblemSolving() != null) {
            total += expertise.getProblemSolving();
            count++;
        }

        return count == 0 ? null : total / count;
    }

    private Double calculateCollaborationAverage(Collaboration collaboration) {
        if (collaboration == null) {
            return null;
        }

        int count = 0;
        double total = 0.0;

        if (collaboration.getCommunication() != null) {
            total += collaboration.getCommunication();
            count++;
        }
        if (collaboration.getScheduleAdherence() != null) {
            total += collaboration.getScheduleAdherence();
            count++;
        }
        if (collaboration.getDispute() != null) {
            total += collaboration.getDispute();
            count++;
        }

        return count == 0 ? null : total / count;
    }

    private void evictRecommendationCaches() {
        deleteByPattern(JOB_RECOMMENDATION_CACHE_KEY_PREFIX + "*");
        deleteByPattern("ai:reco:freelancers:*");
    }

    private void deleteByPattern(String pattern) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(
                    ScanOptions.scanOptions()
                            .match(pattern)
                            .count(SCAN_BATCH_SIZE)
                            .build()
            )) {
                while (cursor.hasNext()) {
                    connection.del(cursor.next());
                }
            } catch (Exception e) {
                log.warn("Failed to evict recommendation backfill cache keys. pattern={}", pattern, e);
            }
            return null;
        });
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
}
