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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    private static final int BACKFILL_BATCH_SIZE = 25;
    private static final String JOB_RECOMMENDATION_CACHE_KEY_PREFIX = "ai:reco:jobs:v2:";
    private static final String BACKFILL_DONE_KEY = "ai:backfill:recommendation:done";
    private static final String BACKFILL_LOCK_KEY = "ai:backfill:recommendation:lock";
    private static final String BACKFILL_JOB_CHECKPOINT_KEY = "ai:backfill:recommendation:checkpoint:job-posting";
    private static final String BACKFILL_FREELANCER_CHECKPOINT_KEY = "ai:backfill:recommendation:checkpoint:freelancer";
    private static final Duration BACKFILL_LOCK_TTL = Duration.ofMinutes(5);

    private final JobPostingRepo jobPostingRepo;
    private final FreelancerRepository freelancerRepository;
    private final StringRedisTemplate redisTemplate;

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
            BackfillCounters counters = new BackfillCounters();

            processJobPostings(restClient, lockToken, counters);
            processFreelancers(restClient, lockToken, counters);

            evictRecommendationCaches();
            redisTemplate.opsForValue().set(
                    BACKFILL_DONE_KEY,
                    "triggeredByUserId=%s,jobSuccess=%s,jobFailed=%s,freelancerSuccess=%s,freelancerFailed=%s".formatted(
                            event.userId(),
                            counters.jobSuccess,
                            counters.jobFailed,
                            counters.freelancerSuccess,
                            counters.freelancerFailed
                    )
            );
            clearCheckpoints();

            log.info(
                    "Recommendation login backfill done. triggeredByUserId={}, jobSuccess={}, jobFailed={}, freelancerSuccess={}, freelancerFailed={}",
                    event.userId(),
                    counters.jobSuccess,
                    counters.jobFailed,
                    counters.freelancerSuccess,
                    counters.freelancerFailed
            );
        } catch (BackfillLockLostException e) {
            log.warn("Recommendation login backfill stopped because lock ownership was lost. triggeredByUserId={}", event.userId(), e);
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

    private void processJobPostings(RestClient restClient, String lockToken, BackfillCounters counters) {
        Long checkpointId = readCheckpoint(BACKFILL_JOB_CHECKPOINT_KEY);
        Long lastFetchedId = checkpointId;

        while (true) {
            renewLockOrThrow(BACKFILL_LOCK_KEY, lockToken);

            List<JobPosting> batch = jobPostingRepo.findByIdGreaterThanOrderByIdAsc(
                    lastFetchedId,
                    PageRequest.of(0, BACKFILL_BATCH_SIZE)
            );
            if (batch.isEmpty()) {
                return;
            }

            for (JobPosting jobPosting : batch) {
                renewLockOrThrow(BACKFILL_LOCK_KEY, lockToken);
                lastFetchedId = jobPosting.getId();

                try {
                    syncData(
                            restClient,
                            jobPosting.getId(),
                            jobPosting.getId(),
                            "job_posting",
                            buildJobPostingAiContent(jobPosting),
                            resolveJobPostingAiStatus(jobPosting)
                    );
                    counters.jobSuccess++;
                    saveCheckpoint(BACKFILL_JOB_CHECKPOINT_KEY, jobPosting.getId());
                } catch (Exception e) {
                    counters.jobFailed++;
                    log.error("Recommendation login backfill failed for job_posting. jobPostingId={}", jobPosting.getId(), e);
                }
            }
        }
    }

    private void processFreelancers(RestClient restClient, String lockToken, BackfillCounters counters) {
        Long checkpointId = readCheckpoint(BACKFILL_FREELANCER_CHECKPOINT_KEY);
        Long lastFetchedId = checkpointId;

        while (true) {
            renewLockOrThrow(BACKFILL_LOCK_KEY, lockToken);

            List<Freelancer> batch = freelancerRepository.findByFreelancerIdGreaterThanOrderByFreelancerIdAsc(
                    lastFetchedId,
                    PageRequest.of(0, BACKFILL_BATCH_SIZE)
            );
            if (batch.isEmpty()) {
                return;
            }

            for (Freelancer freelancer : batch) {
                renewLockOrThrow(BACKFILL_LOCK_KEY, lockToken);
                lastFetchedId = freelancer.getFreelancerId();

                try {
                    syncData(
                            restClient,
                            freelancer.getFreelancerId(),
                            freelancer.getFreelancerId(),
                            "new_profile",
                            buildFreelancerProfileAiContent(freelancer),
                            Optional.ofNullable(freelancer.getStatus()).map(Enum::name).orElse("POTENTIAL")
                    );
                    counters.freelancerSuccess++;
                    saveCheckpoint(BACKFILL_FREELANCER_CHECKPOINT_KEY, freelancer.getFreelancerId());
                } catch (Exception e) {
                    counters.freelancerFailed++;
                    log.error("Recommendation login backfill failed for new_profile. freelancerId={}", freelancer.getFreelancerId(), e);
                }
            }
        }
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

    private void saveCheckpoint(String checkpointKey, Long lastSuccessfulId) {
        if (lastSuccessfulId == null) {
            return;
        }

        redisTemplate.opsForValue().set(checkpointKey, lastSuccessfulId.toString());
    }

    private Long readCheckpoint(String checkpointKey) {
        Object checkpoint = redisTemplate.opsForValue().get(checkpointKey);
        if (checkpoint == null) {
            return 0L;
        }

        if (checkpoint instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(checkpoint.toString());
        } catch (NumberFormatException e) {
            log.warn("Recommendation backfill checkpoint is invalid. key={}, value={}", checkpointKey, checkpoint);
            redisTemplate.delete(checkpointKey);
            return 0L;
        }
    }

    private void clearCheckpoints() {
        redisTemplate.delete(List.of(
                BACKFILL_JOB_CHECKPOINT_KEY,
                BACKFILL_FREELANCER_CHECKPOINT_KEY
        ));
    }

    private void renewLockOrThrow(String lockKey, String lockToken) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end";

        Long renewed = redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                java.util.Collections.singletonList(lockKey),
                lockToken,
                String.valueOf(BACKFILL_LOCK_TTL.toMillis())
        );

        if (!Long.valueOf(1L).equals(renewed)) {
            throw new BackfillLockLostException("Recommendation backfill lock was lost or could not be renewed.");
        }
    }

    private void releaseLockIfOwned(String lockKey, String lockToken) {
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                    new DefaultRedisScript<>(script, Long.class),
                    java.util.Collections.singletonList(lockKey),
                    lockToken
            );
        } catch (Exception ignored) {
        }
    }

    private static final class BackfillCounters {
        private int jobSuccess;
        private int jobFailed;
        private int freelancerSuccess;
        private int freelancerFailed;
    }

    private static final class BackfillLockLostException extends RuntimeException {
        private BackfillLockLostException(String message) {
            super(message);
        }
    }
}
