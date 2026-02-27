package com.fallguys.review.repository;

import com.fallguys.review.entity.FreelancerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FreelancerReviewRepository extends JpaRepository<FreelancerReview, Long> {

    Page<FreelancerReview> findAllByEmployerIdAndDeletedFalseOrderByCreatedAtDesc(Long employerId, Pageable pageable);

    Page<FreelancerReview> findAllByFreelancerIdAndDeletedFalseOrderByCreatedAtDesc(Long freelancerId, Pageable pageable);

    Optional<FreelancerReview> findByIdAndFreelancerIdAndDeletedFalse(Long reviewId, Long freelancerId);

    Optional<FreelancerReview> findByProjectIdAndFreelancerIdAndEmployerIdAndDeletedFalse(
            Long projectId,
            Long freelancerId,
            Long employerId
    );
}
