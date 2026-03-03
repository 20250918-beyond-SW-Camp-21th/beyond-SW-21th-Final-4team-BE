package com.fallguys.review.repository;

import com.fallguys.review.entity.EmployerReview;
import com.fallguys.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerReviewRepository extends JpaRepository<EmployerReview, Long> {

    Page<EmployerReview> findAllByFreelancerIdAndStatusOrderByCreatedAtDesc(
            Long freelancerId,
            ReviewStatus status,
            Pageable pageable
    );

    Page<EmployerReview> findAllByEmployerIdAndStatusOrderByCreatedAtDesc(
            Long employerId,
            ReviewStatus status,
            Pageable pageable
    );

    Optional<EmployerReview> findByIdAndEmployerIdAndStatus(Long reviewId, Long employerId, ReviewStatus status);

    Optional<EmployerReview> findByProjectIdAndEmployerIdAndFreelancerIdAndStatus(
            Long projectId,
            Long employerId,
            Long freelancerId,
            ReviewStatus status
    );
}
