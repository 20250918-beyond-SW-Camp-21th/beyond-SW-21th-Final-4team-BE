package com.fallguys.review.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.review.api.dto.request.EmployerReviewCreateRequest;
import com.fallguys.review.api.dto.request.EmployerReviewUpdateRequest;
import com.fallguys.review.api.dto.request.FreelancerReviewCreateRequest;
import com.fallguys.review.api.dto.request.FreelancerReviewUpdateRequest;
import com.fallguys.review.entity.EmployerReview;
import com.fallguys.review.entity.FreelancerReview;
import com.fallguys.review.entity.ReviewStatus;
import com.fallguys.review.repository.EmployerReviewRepository;
import com.fallguys.review.repository.FreelancerReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private static final String EMPLOYER_REVIEW_RATES_KEY_PREFIX = "employer:review:rates:";
    private static final String FREELANCER_REVIEW_RATES_KEY_PREFIX = "freelancer:review:rates:";

    private final EmployerReviewRepository employerReviewRepository;
    private final FreelancerReviewRepository freelancerReviewRepository;
    private final RedisTemplate<String, Object> redisTemplate;

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
        employerReviewRepository
                .findByProjectIdAndEmployerIdAndFreelancerIdAndStatus(
                        request.projectId(),
                        employerId,
                        request.freelancerId(),
                        ReviewStatus.ACTIVE
                )
                .ifPresent(review -> {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
                });

        EmployerReview review = EmployerReview.builder()
                .projectId(request.projectId())
                .employerId(employerId)
                .freelancerId(request.freelancerId())
                .language(request.language())
                .framework(request.framework())
                .debugging(request.debugging())
                .communication(request.communication())
                .schedule(request.schedule())
                .dispute(request.dispute())
                .description(request.description())
                .build();

        try {
            Long reviewId = employerReviewRepository.save(review).getId();
            runAfterCommitSafely(() -> refreshFreelancerReviewRates(request.freelancerId()));
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
        runAfterCommitSafely(() -> refreshFreelancerReviewRates(review.getFreelancerId()));
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
        runAfterCommitSafely(() -> refreshFreelancerReviewRates(review.getFreelancerId()));
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
            runAfterCommitSafely(() -> refreshEmployerReviewRates(request.employerId()));
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
        runAfterCommitSafely(() -> refreshEmployerReviewRates(review.getEmployerId()));
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
        runAfterCommitSafely(() -> refreshEmployerReviewRates(review.getEmployerId()));
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

    private void refreshEmployerReviewRates(Long employerId) {
        List<FreelancerReview> reviews = orEmpty(freelancerReviewRepository.findAllByEmployerIdAndStatus(employerId, ReviewStatus.ACTIVE));
        List<Map<String, Object>> payload = new ArrayList<>(reviews.size());
        for (FreelancerReview review : reviews) {
            Map<String, Object> item = new HashMap<>();
            item.put("atmosphereRate", toNumberOrZero(review.getAtmosphere()));
            item.put("requirementsDetailRate", toNumberOrZero(review.getRequirementDetail()));
            item.put("scheduleAdherenceRate", toNumberOrZero(review.getSchedule()));
            payload.add(item);
        }
        writeRedisValue(EMPLOYER_REVIEW_RATES_KEY_PREFIX + employerId, payload);
    }

    private void refreshFreelancerReviewRates(Long freelancerId) {
        List<EmployerReview> reviews = orEmpty(employerReviewRepository.findAllByFreelancerIdAndStatus(freelancerId, ReviewStatus.ACTIVE));
        List<Map<String, Object>> payload = new ArrayList<>(reviews.size());
        for (EmployerReview review : reviews) {
            Map<String, Object> item = new HashMap<>();
            item.put("expertiseRate", average(review.getLanguage(), review.getFramework(), review.getDebugging()));
            item.put("communicationRate", toNumberOrZero(review.getCommunication()));
            item.put("scheduleRate", toNumberOrZero(review.getSchedule()));
            payload.add(item);
        }
        writeRedisValue(FREELANCER_REVIEW_RATES_KEY_PREFIX + freelancerId, payload);
    }

    private Number toNumberOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double average(Integer... values) {
        int sum = 0;
        int count = 0;
        for (Integer value : values) {
            if (value == null) {
                continue;
            }
            sum += value;
            count++;
        }
        if (count == 0) {
            return 0.0;
        }
        return (double) sum / count;
    }

    private void writeRedisValue(String key, Object value) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (RuntimeException e) {
            log.warn("Failed to write mypage review payload. key={}", key, e);
        }
    }

    private <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
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
}
