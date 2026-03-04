package com.fallguys.review.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.review.api.dto.request.FreelancerReviewCreateRequest;
import com.fallguys.review.api.dto.request.FreelancerReviewUpdateRequest;
import com.fallguys.review.api.dto.response.EmployerReviewResponseDTO;
import com.fallguys.review.api.dto.response.FreelancerReviewResponseDTO;
import com.fallguys.review.api.dto.response.PagedResponseDTO;
import com.fallguys.review.api.support.ReviewTokenUserIdResolver;
import com.fallguys.review.entity.EmployerReview;
import com.fallguys.review.entity.FreelancerReview;
import com.fallguys.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("reviewFreelancerReviewController")
@RequiredArgsConstructor
@Tag(name = "Review - Freelancer", description = "프리랜서 리뷰 조회 및 작성/수정/삭제 API")
public class FreelancerReviewController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewService reviewService;
    private final ReviewTokenUserIdResolver reviewTokenUserIdResolver;

    @Operation(summary = "프리랜서가 리뷰 작성", description = "프리랜서가 프로젝트에 대한 후기를 작성합니다.")
    @PostMapping("/api/freelancer/projects/{projectId}/reviews")
    public ResponseEntity<ApiResponse<Long>> createFreelancerReview(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long projectId,
            @Valid @RequestBody FreelancerReviewCreateRequest request
    ) {
        Long freelancerId = reviewTokenUserIdResolver.resolveUserId(authorization);
        FreelancerReviewCreateRequest merged = new FreelancerReviewCreateRequest(
                projectId,
                request.employerId(),
                request.atmosphere(),
                request.requirementDetail(),
                request.schedule(),
                request.description()
        );
        Long reviewId = reviewService.createFreelancerReview(freelancerId, merged);
        return ResponseEntity.ok(ApiResponse.ok(reviewId));
    }

    @Operation(summary = "프리랜서가 작성한 리뷰 수정", description = "프리랜서가 작성한 리뷰를 수정합니다.")
    @PutMapping("/api/freelancer/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> updateFreelancerReview(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long reviewId,
            @Valid @RequestBody FreelancerReviewUpdateRequest request
    ) {
        Long freelancerId = reviewTokenUserIdResolver.resolveUserId(authorization);
        reviewService.updateFreelancerReview(freelancerId, reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "프리랜서가 작성한 리뷰 삭제", description = "프리랜서가 작성한 리뷰를 삭제합니다.")
    @DeleteMapping("/api/freelancer/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteFreelancerReview(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long reviewId
    ) {
        Long freelancerId = reviewTokenUserIdResolver.resolveUserId(authorization);
        reviewService.deleteFreelancerReview(freelancerId, reviewId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "프리랜서가 받은 전체 리뷰 조회", description = "고용주가 프리랜서에게 작성한 리뷰 목록을 조회합니다.")
    @GetMapping("/api/freelancer/reviews")
    public ResponseEntity<ApiResponse<PagedResponseDTO<EmployerReviewResponseDTO>>> getFreelancerReceivedReviews(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long freelancerId = reviewTokenUserIdResolver.resolveUserId(authorization);

        Page<EmployerReview> result = reviewService.getFreelancerReceivedReviews(freelancerId, PageRequest.of(safePage, safeSize));
        List<EmployerReviewResponseDTO> content = result.getContent()
                .stream()
                .map(EmployerReviewResponseDTO::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(new PagedResponseDTO<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        )));
    }

    @Operation(summary = "프리랜서가 작성한 전체 리뷰 조회", description = "프리랜서가 고용주에게 작성한 리뷰 목록을 조회합니다.")
    @GetMapping("/api/freelancer/reviews/written")
    public ResponseEntity<ApiResponse<PagedResponseDTO<FreelancerReviewResponseDTO>>> getFreelancerWrittenReviews(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long freelancerId = reviewTokenUserIdResolver.resolveUserId(authorization);

        Page<FreelancerReview> result = reviewService.getFreelancerWrittenReviews(freelancerId, PageRequest.of(safePage, safeSize));
        List<FreelancerReviewResponseDTO> content = result.getContent()
                .stream()
                .map(FreelancerReviewResponseDTO::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(new PagedResponseDTO<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        )));
    }
}
