package com.fallguys.review.repository;

import com.fallguys.review.entity.EmployerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerReviewRepository extends JpaRepository<EmployerReview, Long> {

    Page<EmployerReview> findAllByFreelancerIdAndDeletedFalseOrderByCreatedAtDesc(Long freelancerId, Pageable pageable);

    Page<EmployerReview> findAllByEmployerIdAndDeletedFalseOrderByCreatedAtDesc(Long employerId, Pageable pageable);

    Optional<EmployerReview> findByIdAndEmployerIdAndDeletedFalse(Long reviewId, Long employerId);

    Optional<EmployerReview> findByProjectIdAndEmployerIdAndFreelancerIdAndDeletedFalse(
            Long projectId,
            Long employerId,
            Long freelancerId
    );
}
