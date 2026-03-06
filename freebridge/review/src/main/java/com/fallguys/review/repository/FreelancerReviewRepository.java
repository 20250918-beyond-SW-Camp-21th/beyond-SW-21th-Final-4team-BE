package com.fallguys.review.repository;

import com.fallguys.review.entity.FreelancerReview;
import com.fallguys.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FreelancerReviewRepository extends JpaRepository<FreelancerReview, Long> {

    Page<FreelancerReview> findAllByEmployerIdAndStatusOrderByCreatedAtDesc(
            Long employerId,
            ReviewStatus status,
            Pageable pageable
    );

    Page<FreelancerReview> findAllByFreelancerIdAndStatusOrderByCreatedAtDesc(
            Long freelancerId,
            ReviewStatus status,
            Pageable pageable
    );

    Optional<FreelancerReview> findByIdAndFreelancerIdAndStatus(Long reviewId, Long freelancerId, ReviewStatus status);

    Optional<FreelancerReview> findByProjectIdAndFreelancerIdAndEmployerIdAndStatus(
            Long projectId,
            Long freelancerId,
            Long employerId,
            ReviewStatus status
    );

    List<FreelancerReview> findAllByEmployerIdAndStatus(Long employerId, ReviewStatus status);
}
