package com.fallguys.review.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.review.api.dto.response.EmployerRejectionReasonResponseDTO;
import com.fallguys.review.api.dto.response.PagedResponseDTO;
import com.fallguys.review.api.support.ReviewTokenUserIdResolver;
import com.fallguys.review.service.EmployerRejectionReasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("reviewFreelancerRejectionReasonController")
@RequiredArgsConstructor
@Tag(name = "Review - Freelancer Rejection Reason", description = "프리랜서 거절 사유 조회 API")
public class FreelancerRejectionReasonController {

    private static final int MAX_PAGE_SIZE = 100;

    private final EmployerRejectionReasonService employerRejectionReasonService;
    private final ReviewTokenUserIdResolver reviewTokenUserIdResolver;

    @Operation(summary = "프리랜서 거절 사유 목록 조회", description = "프리랜서가 자신이 거절당한 사유를 전체 조회하거나 프로젝트 제목으로 검색합니다.")
    @GetMapping("/api/freelancer/rejection-reasons")
    public ResponseEntity<ApiResponse<PagedResponseDTO<EmployerRejectionReasonResponseDTO>>> getFreelancerRejectionReasons(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long freelancerId = reviewTokenUserIdResolver.resolveUserId(authorization);

        Page<EmployerRejectionReasonResponseDTO> result = employerRejectionReasonService.getFreelancerRejectionReasons(
                freelancerId,
                title,
                PageRequest.of(safePage, safeSize)
        );
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponseDTO<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        )));
    }
}
