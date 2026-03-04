package com.fallguys.review.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.review.api.dto.request.EmployerReviewCreateRequest;
import com.fallguys.review.api.dto.request.EmployerReviewUpdateRequest;
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

@RestController("reviewEmployerReviewController")
@RequiredArgsConstructor
@Tag(name = "Review - Employer", description = "고용주 리뷰 조회 및 작성/수정/삭제 API")
public class EmployerReviewController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewService reviewService;
    private final ReviewTokenUserIdResolver reviewTokenUserIdResolver;

    @Operation(summary = "고용주가 받은 전체 후기 조회", description = "프리랜서가 고용주에게 작성한 후기 목록을 조회합니다.")
    @GetMapping("/api/employer/reviews")
    public ResponseEntity<ApiResponse<PagedResponseDTO<FreelancerReview>>> getEmployerReceivedReviews(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long employerId = reviewTokenUserIdResolver.resolveUserId(authorization);

        Page<FreelancerReview> result = reviewService.getEmployerReceivedReviews(employerId, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponseDTO<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        )));
    }

    @Operation(summary = "고용주가 작성한 전체 후기 조회", description = "고용주가 프리랜서에게 작성한 후기 목록을 조회합니다.")
    @GetMapping("/api/employer/reviews/written")
    public ResponseEntity<ApiResponse<PagedResponseDTO<EmployerReview>>> getEmployerWrittenReviews(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long employerId = reviewTokenUserIdResolver.resolveUserId(authorization);

        Page<EmployerReview> result = reviewService.getEmployerWrittenReviews(employerId, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponseDTO<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        )));
    }

    @Operation(summary = "고용주 후기 작성", description = "고용주가 프로젝트에 대한 후기를 작성합니다.")
    @PostMapping("/api/employer/projects/{projectId}/reviews")
    public ResponseEntity<ApiResponse<Long>> createEmployerReview(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long projectId,
            @Valid @RequestBody EmployerReviewCreateRequest request
    ) {
        Long employerId = reviewTokenUserIdResolver.resolveUserId(authorization);
        EmployerReviewCreateRequest merged = new EmployerReviewCreateRequest(
                projectId,
                request.freelancerId(),
                request.language(),
                request.framework(),
                request.debugging(),
                request.communication(),
                request.schedule(),
                request.dispute(),
                request.description()
        );
        Long reviewId = reviewService.createEmployerReview(employerId, merged);
        return ResponseEntity.ok(ApiResponse.ok(reviewId));
    }

    @Operation(summary = "고용주 후기 수정", description = "고용주가 작성한 후기를 수정합니다.")
    @PutMapping("/api/employer/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> updateEmployerReview(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long reviewId,
            @Valid @RequestBody EmployerReviewUpdateRequest request
    ) {
        Long employerId = reviewTokenUserIdResolver.resolveUserId(authorization);
        reviewService.updateEmployerReview(employerId, reviewId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "고용주 후기 삭제", description = "고용주가 작성한 후기를 삭제합니다.")
    @DeleteMapping("/api/employer/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployerReview(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long reviewId
    ) {
        Long employerId = reviewTokenUserIdResolver.resolveUserId(authorization);
        reviewService.deleteEmployerReview(employerId, reviewId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
