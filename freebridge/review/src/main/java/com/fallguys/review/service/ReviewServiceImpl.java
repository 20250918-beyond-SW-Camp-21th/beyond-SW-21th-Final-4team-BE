package com.fallguys.review.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.port.ProjectExternalApi;
import com.fallguys.review.api.dto.request.EmployerReviewCreateRequest;
import com.fallguys.review.api.dto.request.EmployerReviewUpdateRequest;
import com.fallguys.review.api.dto.request.FreelancerReviewCreateRequest;
import com.fallguys.review.api.dto.request.FreelancerReviewUpdateRequest;
import com.fallguys.review.entity.EmployerReview;
import com.fallguys.review.entity.FreelancerReview;
import com.fallguys.review.entity.ReviewStatus;
import com.fallguys.review.repository.EmployerReviewRepository;
import com.fallguys.review.repository.FreelancerReviewRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private static final String EMPLOYER_REVIEW_RATES_KEY_PREFIX = "employer:review:rates:";
    private static final String FREELANCER_REVIEW_RATES_KEY_PREFIX = "freelancer:review:rates:";

    private final EmployerReviewRepository employerReviewRepository;
    private final FreelancerReviewRepository freelancerReviewRepository;
    private final ProjectExternalApi projectExternalApi;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    @Override
    public Page<FreelancerReview> getEmployerReceivedReviews(Long employerId, Pageable pageable) {
        return freelancerReviewRepository.findAllByEmployerIdAndStatusOrderByCreatedAtDesc(
                employerId,
                ReviewStatus.ACTIVE,
                pageable
        );
    }

    @Override
    public Page<EmployerReview> getEmployerWrittenReviews(Long employerId, Pageable pageable) {
        return employerReviewRepository.findAllByEmployerIdAndStatusOrderByCreatedAtDesc(
                employerId,
                ReviewStatus.ACTIVE,
                pageable
        );
    }

    @Override
    @Transactional
    public Long createEmployerReview(Long employerId, EmployerReviewCreateRequest request) {
        FreelancerIdentity freelancerIdentity = resolveFreelancerIdentity(request.freelancerId());
        Long normalizedFreelancerUserId = freelancerIdentity.userId();

        assertNoDuplicateEmployerReview(request.projectId(), employerId, normalizedFreelancerUserId);

        EmployerReview review = EmployerReview.builder()
                .projectId(request.projectId())
                .employerId(employerId)
                .freelancerId(normalizedFreelancerUserId)
                .language(request.language())
                .framework(request.framework())
                .debugging(request.debugging())
                .communication(request.communication())
                .schedule(request.schedule())
                .dispute(request.dispute())
                .description(request.description())
                .build();

        try {
            EmployerReview savedReview = employerReviewRepository.save(review);
            Long reviewId = savedReview.getId();

            runAfterCommitSafely(() -> invalidateFreelancerReviewCaches(freelancerIdentity));

            projectExternalApi.completeProjectWithReview(
                    new ProjectExternalApi.ProjectCompletionData(
                            savedReview.getProjectId(),
                            request.freelancerId(),
                            savedReview.getDescription(),
                            savedReview.getCommunication() != null ? savedReview.getCommunication() : 0,
                            savedReview.getDebugging() != null ? savedReview.getDebugging() : 0,
                            savedReview.getFramework() != null ? savedReview.getFramework() : 0,
                            savedReview.getLanguage() != null ? savedReview.getLanguage() : 0,
                            savedReview.getSchedule() != null ? savedReview.getSchedule() : 0
                    )
            );

            return reviewId;

        } catch (DataIntegrityViolationException e) {
            if (!isDuplicateKeyViolation(e)) {
                throw e;
            }
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    @Transactional
    public void updateEmployerReview(Long employerId, Long reviewId, EmployerReviewUpdateRequest request) {
        EmployerReview review = employerReviewRepository.findByIdAndEmployerIdAndStatus(
                        reviewId,
                        employerId,
                        ReviewStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        review.update(
                request.language(),
                request.framework(),
                request.debugging(),
                request.communication(),
                request.schedule(),
                request.dispute(),
                request.description()
        );
        FreelancerIdentity freelancerIdentity = resolveFreelancerIdentity(review.getFreelancerId());
        runAfterCommitSafely(() -> invalidateFreelancerReviewCaches(freelancerIdentity));
    }

    @Override
    @Transactional
    public void deleteEmployerReview(Long employerId, Long reviewId) {
        EmployerReview review = employerReviewRepository.findByIdAndEmployerIdAndStatus(
                        reviewId,
                        employerId,
                        ReviewStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        review.softDelete();
        FreelancerIdentity freelancerIdentity = resolveFreelancerIdentity(review.getFreelancerId());
        runAfterCommitSafely(() -> invalidateFreelancerReviewCaches(freelancerIdentity));
    }

    @Override
    public Page<EmployerReview> getFreelancerReceivedReviews(Long freelancerId, Pageable pageable) {
        return employerReviewRepository.findAllByFreelancerIdAndStatusOrderByCreatedAtDesc(
                freelancerId,
                ReviewStatus.ACTIVE,
                pageable
        );
    }

    @Override
    public Page<FreelancerReview> getFreelancerWrittenReviews(Long freelancerId, Pageable pageable) {
        return freelancerReviewRepository.findAllByFreelancerIdAndStatusOrderByCreatedAtDesc(
                freelancerId,
                ReviewStatus.ACTIVE,
                pageable
        );
    }

    @Override
    @Transactional
    public Long createFreelancerReview(Long freelancerId, FreelancerReviewCreateRequest request) {
        freelancerReviewRepository
                .findByProjectIdAndFreelancerIdAndEmployerIdAndStatus(
                        request.projectId(),
                        freelancerId,
                        request.employerId(),
                        ReviewStatus.ACTIVE
                )
                .ifPresent(review -> {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
                });

        FreelancerReview review = FreelancerReview.builder()
                .projectId(request.projectId())
                .freelancerId(freelancerId)
                .employerId(request.employerId())
                .atmosphere(request.atmosphere())
                .requirementDetail(request.requirementDetail())
                .schedule(request.schedule())
                .description(request.description())
                .build();

        try {
            Long reviewId = freelancerReviewRepository.save(review).getId();
            runAfterCommitSafely(() -> invalidateEmployerReviewCache(request.employerId()));
            return reviewId;
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicateKeyViolation(e)) {
                throw e;
            }
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Override
    @Transactional
    public void updateFreelancerReview(Long freelancerId, Long reviewId, FreelancerReviewUpdateRequest request) {
        FreelancerReview review = freelancerReviewRepository.findByIdAndFreelancerIdAndStatus(
                        reviewId,
                        freelancerId,
                        ReviewStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        review.update(
                request.atmosphere(),
                request.requirementDetail(),
                request.schedule(),
                request.description()
        );
        runAfterCommitSafely(() -> invalidateEmployerReviewCache(review.getEmployerId()));
    }

    @Override
    @Transactional
    public void deleteFreelancerReview(Long freelancerId, Long reviewId) {
        FreelancerReview review = freelancerReviewRepository.findByIdAndFreelancerIdAndStatus(
                        reviewId,
                        freelancerId,
                        ReviewStatus.ACTIVE
                )
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT_VALUE));
        review.softDelete();
        runAfterCommitSafely(() -> invalidateEmployerReviewCache(review.getEmployerId()));
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
                log.warn("Failed to refresh mypage review redis payload after commit", e);
            }
        });
    }

    private void invalidateEmployerReviewCache(Long employerId) {
        deleteRedisValue(EMPLOYER_REVIEW_RATES_KEY_PREFIX + employerId);
    }

    private void invalidateFreelancerReviewCaches(FreelancerIdentity freelancerIdentity) {
        deleteRedisValue(FREELANCER_REVIEW_RATES_KEY_PREFIX + freelancerIdentity.freelancerPk());
        deleteRedisValue(FREELANCER_REVIEW_RATES_KEY_PREFIX + freelancerIdentity.userId());
        deleteRedisValue("freelancer:review:ai_report:" + freelancerIdentity.freelancerPk());
        deleteRedisValue("freelancer:review:ai_report:" + freelancerIdentity.userId());

        try {
            eventPublisher.publishEvent(
                    new com.fallguys.common.event.ReputationUpdateRequestedEvent(freelancerIdentity.freelancerPk())
            );
        } catch (RuntimeException e) {
            log.warn("Failed to publish reputation update event. freelancerId={}", freelancerIdentity.freelancerPk(), e);
        }
    }

    private void assertNoDuplicateEmployerReview(Long projectId, Long employerId, Long freelancerId) {
        employerReviewRepository
                .findByProjectIdAndEmployerIdAndFreelancerIdAndStatus(
                        projectId,
                        employerId,
                        freelancerId,
                        ReviewStatus.ACTIVE
                )
                .ifPresent(review -> {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
                });
    }

    private FreelancerIdentity resolveFreelancerIdentity(Long freelancerReferenceId) {
        if (freelancerReferenceId == null) {
            return new FreelancerIdentity(null, null);
        }

        try {
            Query query = entityManager.createNativeQuery("""
                    SELECT freelancer_id, user_id
                    FROM freelancer
                    WHERE freelancer_id = :referenceId
                       OR user_id = :referenceId
                    LIMIT 1
                    """);
            query.setParameter("referenceId", freelancerReferenceId);
            Object singleResult = query.getSingleResult();
            if (singleResult instanceof Object[] row && row.length >= 2) {
                Long freelancerPk = toLong(row[0]);
                Long userId = toLong(row[1]);
                return new FreelancerIdentity(
                        freelancerPk != null ? freelancerPk : freelancerReferenceId,
                        userId != null ? userId : freelancerReferenceId
                );
            }
        } catch (RuntimeException e) {
            log.debug("Failed to resolve freelancer identity. freelancerReferenceId={}", freelancerReferenceId, e);
        }

        return new FreelancerIdentity(freelancerReferenceId, freelancerReferenceId);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }


    private void deleteRedisValue(String key) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException e) {
            log.warn("Failed to delete mypage review payload. key={}", key, e);
        }
    }
    private boolean isDuplicateKeyViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if ("23505".equals(sqlState)) {
                    return true;
                }
                if (sqlException.getErrorCode() == 1062) {
                    return true;
                }
            }

            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("duplicate")) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }

    private record FreelancerIdentity(Long freelancerPk, Long userId) {
    }
}
