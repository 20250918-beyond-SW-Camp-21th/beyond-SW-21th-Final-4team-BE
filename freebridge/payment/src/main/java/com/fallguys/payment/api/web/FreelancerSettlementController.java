package com.fallguys.payment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.FreelancerSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Freelancer Settlement", description = "프리랜서 정산 관련 API")
@RestController
@RequestMapping("/api/v1/settlements/freelancer")
@RequiredArgsConstructor
public class FreelancerSettlementController {

    private final FreelancerSettlementService freelancerSettlementService;

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "프리랜서 정산 목록 조회", description = "상태/날짜범위/검색어로 필터링 가능한 페이지네이션 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FreelancerSettlementItem>>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "ALL") String dateRange,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "SCHEDULED_DATE_ASC") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<FreelancerSettlementItem> response =
                freelancerSettlementService.listSettlements(userId, status, dateRange, search, sort, page, size);
        ApiResponse<PageResponse<FreelancerSettlementItem>> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "프리랜서 정산 통계 조회", description = "지급 예정 금액, 지급 완료 금액 집계")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<FreelancerSettlementSummaryResponse>> summary(
            @RequestHeader("X-User-Id") Long userId) {

        FreelancerSettlementSummaryResponse response = freelancerSettlementService.getSummary(userId);
        ApiResponse<FreelancerSettlementSummaryResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "프리랜서 정산 상세 조회")
    @GetMapping("/{settlementId}")
    public ResponseEntity<ApiResponse<FreelancerSettlementDetailResponse>> detail(
            @PathVariable Long settlementId,
            @RequestHeader("X-User-Id") Long userId) {

        FreelancerSettlementDetailResponse response =
                freelancerSettlementService.getSettlementDetail(userId, settlementId);
        ApiResponse<FreelancerSettlementDetailResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "지급 영수증 PDF 다운로드", description = "S3 pre-signed URL 또는 redirect로 반환")
    @GetMapping("/{settlementId}/receipt")
    public ResponseEntity<ApiResponse<String>> receipt(
            @PathVariable Long settlementId,
            @RequestHeader("X-User-Id") Long userId) {

        String pdfUrl = freelancerSettlementService.getReceiptPdfUrl(userId, settlementId);
        ApiResponse<String> apiResponse = ApiResponse.ok(pdfUrl);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "세금계산서 발행 요청",
            description = "status=PAID인 정산에 대해서만 허용. 이미 요청된 경우 409 반환")
    @PostMapping("/{settlementId}/tax-invoice")
    public ResponseEntity<ApiResponse<TaxInvoiceResponse>> requestTaxInvoice(
            @PathVariable Long settlementId,
            @Valid @RequestBody TaxInvoiceRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        TaxInvoiceResponse response = freelancerSettlementService.requestTaxInvoice(userId, settlementId, request);
        ApiResponse<TaxInvoiceResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
