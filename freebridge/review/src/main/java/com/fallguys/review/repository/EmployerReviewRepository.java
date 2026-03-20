package com.fallguys.review.repository;

import com.fallguys.review.entity.EmployerReview;
import com.fallguys.review.entity.ReviewStatus;
import com.fallguys.review.api.shared.response.FreelancerReviewMetricsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    List<EmployerReview> findAllByFreelancerIdAndStatus(Long freelancerId, ReviewStatus status);

    @Query(value = """
            SELECT
                AVG(language) AS programming,
                AVG(framework) AS framework,
                AVG(debugging) AS debugging,
                AVG(communication) AS communication,
                AVG(schedule) AS schedule,
                AVG(dispute) AS dispute
            FROM employer_freelancer_reviews
            WHERE status = 'ACTIVE'
              AND deleted = false
              AND freelancer_id = :userId
            """, nativeQuery = true)
    FreelancerReviewMetricsProjection findFreelancerReviewMetrics(@Param("userId") Long userId);
}
