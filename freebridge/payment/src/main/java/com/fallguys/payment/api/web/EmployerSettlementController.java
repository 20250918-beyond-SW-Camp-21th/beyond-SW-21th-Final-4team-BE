package com.fallguys.payment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.EmployerSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Employer Settlement", description = "고용주 정산 관련 API")
@RestController
@RequestMapping("/api/v1/settlements/employer")
@RequiredArgsConstructor
public class EmployerSettlementController {

    private final EmployerSettlementService employerSettlementService;

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "고용주 정산 목록 조회", description = "상태/날짜범위/검색어로 필터링 가능한 페이지네이션 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployerSettlementItem>>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "ALL") String dateRange,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "DUE_DATE_ASC") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<EmployerSettlementItem> response =
                employerSettlementService.listSettlements(userId, status, dateRange, search, sort, page, size);
        ApiResponse<PageResponse<EmployerSettlementItem>> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "고용주 정산 통계 조회", description = "총 지급액, 지급 완료 건수 등 집계 데이터")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<EmployerSettlementSummaryResponse>> summary(
            @RequestHeader("X-User-Id") Long userId) {

        EmployerSettlementSummaryResponse response = employerSettlementService.getSummary(userId);
        ApiResponse<EmployerSettlementSummaryResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "다음 정산 예정 조회", description = "PAID 상태 중 가장 가까운 미지급 회차 반환")
    @GetMapping("/next")
    public ResponseEntity<ApiResponse<EmployerSettlementNextResponse>> next(
            @RequestHeader("X-User-Id") Long userId) {

        EmployerSettlementNextResponse response = employerSettlementService.getNextSettlement(userId);
        ApiResponse<EmployerSettlementNextResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "고용주 정산 상세 조회")
    @GetMapping("/{settlementId}")
    public ResponseEntity<ApiResponse<EmployerSettlementDetailResponse>> detail(
            @PathVariable Long settlementId,
            @RequestHeader("X-User-Id") Long userId) {

        EmployerSettlementDetailResponse response =
                employerSettlementService.getSettlementDetail(userId, settlementId);
        ApiResponse<EmployerSettlementDetailResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "청구서 PDF 다운로드", description = "S3 pre-signed URL 또는 redirect로 반환")
    @GetMapping("/{settlementId}/invoice")
    public ResponseEntity<ApiResponse<String>> invoice(
            @PathVariable Long settlementId,
            @RequestHeader("X-User-Id") Long userId) {

        String pdfUrl = employerSettlementService.getInvoicePdfUrl(userId, settlementId);
        ApiResponse<String> apiResponse = ApiResponse.ok(pdfUrl);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "계약 선불 결제 검증 (PortOne)",
            description = "PortOne 결제 검증 후 계약 활성화 및 정산 레코드 생성. 멱등성 보장: 동일 imp_uid 재호출 시 기존 결과 반환")
    @PostMapping("/verify-payment")
    public ResponseEntity<ApiResponse<VerifyPaymentResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        VerifyPaymentResponse response =
                employerSettlementService.verifyContractPayment(request.getPaymentId(), request.getContractId(), userId);
        ApiResponse<VerifyPaymentResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
