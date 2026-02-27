package com.fallguys.review.service;

import com.fallguys.review.api.dto.request.EmployerReviewCreateRequest;
import com.fallguys.review.api.dto.request.EmployerReviewUpdateRequest;
import com.fallguys.review.api.dto.request.FreelancerReviewCreateRequest;
import com.fallguys.review.api.dto.request.FreelancerReviewUpdateRequest;
import com.fallguys.review.entity.EmployerReview;
import com.fallguys.review.entity.FreelancerReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    Page<FreelancerReview> getEmployerReceivedReviews(Long employerId, Pageable pageable);

    Page<EmployerReview> getEmployerWrittenReviews(Long employerId, Pageable pageable);

    Long createEmployerReview(Long employerId, EmployerReviewCreateRequest request);

    void updateEmployerReview(Long employerId, Long reviewId, EmployerReviewUpdateRequest request);

    void deleteEmployerReview(Long employerId, Long reviewId);

    Page<EmployerReview> getFreelancerReceivedReviews(Long freelancerId, Pageable pageable);

    Page<FreelancerReview> getFreelancerWrittenReviews(Long freelancerId, Pageable pageable);

    Long createFreelancerReview(Long freelancerId, FreelancerReviewCreateRequest request);

    void updateFreelancerReview(Long freelancerId, Long reviewId, FreelancerReviewUpdateRequest request);

    void deleteFreelancerReview(Long freelancerId, Long reviewId);
}
