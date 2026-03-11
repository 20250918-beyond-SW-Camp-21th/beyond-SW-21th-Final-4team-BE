package com.fallguys.review.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.review.api.dto.request.EmployerRejectionReasonCreateRequest;
import com.fallguys.review.api.dto.response.EmployerRejectionReasonResponseDTO;
import com.fallguys.review.api.dto.response.PagedResponseDTO;
import com.fallguys.review.api.support.ReviewTokenUserIdResolver;
import com.fallguys.review.service.EmployerRejectionReasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("reviewEmployerRejectionReasonController")
@RequiredArgsConstructor
@Tag(name = "Review - Employer Rejection Reason", description = "고용주 거절 사유 작성 및 조회 API")
public class EmployerRejectionReasonController {

    private static final int MAX_PAGE_SIZE = 100;

    private final EmployerRejectionReasonService employerRejectionReasonService;
    private final ReviewTokenUserIdResolver reviewTokenUserIdResolver;

    @Operation(summary = "고용주 거절 사유 작성", description = "고용주가 프리랜서 거절 사유를 저장합니다.")
    @PostMapping("/api/employer/rejection-reasons")
    public ResponseEntity<ApiResponse<Long>> createEmployerRejectionReason(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody EmployerRejectionReasonCreateRequest request
    ) {
        Long employerId = reviewTokenUserIdResolver.resolveUserId(authorization);
        Long rejectionReasonId = employerRejectionReasonService.createEmployerRejectionReason(employerId, request);
        return ResponseEntity.ok(ApiResponse.ok(rejectionReasonId));
    }

    @Operation(summary = "고용주 거절 사유 목록 조회", description = "고용주가 등록한 거절 사유를 전체 조회하거나 프로젝트 제목으로 검색합니다.")
    @GetMapping("/api/employer/rejection-reasons")
    public ResponseEntity<ApiResponse<PagedResponseDTO<EmployerRejectionReasonResponseDTO>>> getEmployerRejectionReasons(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long employerId = reviewTokenUserIdResolver.resolveUserId(authorization);

        Page<EmployerRejectionReasonResponseDTO> result = employerRejectionReasonService.getEmployerRejectionReasons(
                employerId,
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
