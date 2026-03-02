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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final EmployerReviewRepository employerReviewRepository;
    private final FreelancerReviewRepository freelancerReviewRepository;

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
            return employerReviewRepository.save(review).getId();
        } catch (DataIntegrityViolationException e) {
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
            return freelancerReviewRepository.save(review).getId();
        } catch (DataIntegrityViolationException e) {
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
    }
}
