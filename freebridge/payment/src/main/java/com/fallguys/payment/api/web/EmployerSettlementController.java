package com.fallguys.payment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.EmployerSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "Employer Settlement", description = "고용주 정산 관련 API")
@RestController
@RequestMapping("/api/settlements/employer")
@RequiredArgsConstructor
public class EmployerSettlementController {

        private final EmployerSettlementService employerSettlementService;

        @Operation(summary = "고용주 정산 목록 조회", description = "상태/날짜범위/검색어로 필터링 가능한 페이지네이션 목록")
        @GetMapping
        public ResponseEntity<ApiResponse<PageResponse<EmployerSettlementItem>>> list(
                        @AuthenticationPrincipal CustomUserDetails user,
                        @RequestParam(defaultValue = "ALL") String status,
                        @RequestParam(defaultValue = "ALL") String dateRange,
                        @RequestParam(defaultValue = "DUE_DATE_ASC") String sort,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size) {

                PageResponse<EmployerSettlementItem> response = employerSettlementService.listSettlements(user.getId(),
                                status, dateRange, sort, page, size);
                ApiResponse<PageResponse<EmployerSettlementItem>> apiResponse = ApiResponse.ok(response);
                return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
        }

        @Operation(summary = "고용주 정산 통계 조회", description = "총 지급액, 지급 완료 건수 등 집계 데이터")
        @GetMapping("/summary")
        public ResponseEntity<ApiResponse<EmployerSettlementSummaryResponse>> summary(
                        @AuthenticationPrincipal CustomUserDetails user) {

                EmployerSettlementSummaryResponse response = employerSettlementService.getSummary(user.getId());
                ApiResponse<EmployerSettlementSummaryResponse> apiResponse = ApiResponse.ok(response);
                return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
        }

        @Operation(summary = "다음 정산 예정 조회", description = "PAID 상태 중 가장 가까운 미지급 회차 반환")
        @GetMapping("/next")
        public ResponseEntity<ApiResponse<EmployerSettlementNextResponse>> next(
                        @AuthenticationPrincipal CustomUserDetails user) {

                EmployerSettlementNextResponse response = employerSettlementService.getNextSettlement(user.getId());
                ApiResponse<EmployerSettlementNextResponse> apiResponse = ApiResponse.ok(response);
                return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
        }

        @Operation(summary = "고용주 정산 상세 조회")
        @GetMapping("/{settlementId}")
        public ResponseEntity<ApiResponse<EmployerSettlementDetailResponse>> detail(
                        @PathVariable Long settlementId,
                        @AuthenticationPrincipal CustomUserDetails user) {

                EmployerSettlementDetailResponse response = employerSettlementService.getSettlementDetail(user.getId(),
                                settlementId);
                ApiResponse<EmployerSettlementDetailResponse> apiResponse = ApiResponse.ok(response);
                return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
        }

        @Operation(summary = "청구서 PDF 다운로드", description = "S3 pre-signed URL 또는 redirect로 반환")
        @GetMapping("/{settlementId}/invoice")
        public ResponseEntity<ApiResponse<String>> invoice(
                        @PathVariable Long settlementId,
                        @AuthenticationPrincipal CustomUserDetails user) {

                String pdfUrl = employerSettlementService.getInvoicePdfUrl(user.getId(), settlementId);
                ApiResponse<String> apiResponse = ApiResponse.ok(pdfUrl);
                return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
        }

        @Operation(summary = "청구서 PDF 다운로드 (redirect)", description = "저장된 PDF URL로 리다이렉트합니다.")
        @GetMapping("/{settlementId}/invoice/download")
        public ResponseEntity<Void> downloadInvoice(
                        @PathVariable Long settlementId,
                        @AuthenticationPrincipal CustomUserDetails user) {

                String pdfUrl = employerSettlementService.getInvoicePdfUrl(user.getId(), settlementId);
                if (pdfUrl == null || pdfUrl.isBlank()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(pdfUrl)).build();
        }

        @Operation(summary = "청구서 PDF 재생성", description = "정산 ID로 서비스 수수료 인보이스를 재생성합니다.")
        @PostMapping("/{settlementId}/invoice/regenerate")
        public ResponseEntity<ApiResponse<String>> regenerateInvoice(
                        @PathVariable Long settlementId,
                        @AuthenticationPrincipal CustomUserDetails user) {

                String pdfUrl = employerSettlementService.regenerateInvoicePdf(user.getId(), settlementId);
                ApiResponse<String> apiResponse = ApiResponse.ok(pdfUrl);
                return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
        }

        @Operation(summary = "계약 선불 결제 검증 (PortOne)", description = "PortOne 결제 검증 후 계약 활성화 및 정산 레코드 생성. 멱등성 보장: 동일 imp_uid 재호출 시 기존 결과 반환")
        @PostMapping("/verify-payment")
        public ResponseEntity<ApiResponse<VerifyPaymentResponse>> verifyPayment(
                        @Valid @RequestBody VerifyPaymentRequest request,
                        @AuthenticationPrincipal CustomUserDetails user) {

                VerifyPaymentResponse response = employerSettlementService.verifyContractPayment(request.getPaymentId(),
                                request.getContractId(), user.getId());
                ApiResponse<VerifyPaymentResponse> apiResponse = ApiResponse.ok(response);
                return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
        }

        /*
         * @Operation(summary = "계약 취소 및 환불 요청",
         * description = """
         * PENDING(에스크로 보관) 상태인 회차를 취소하고 PortOne 환불을 처리합니다.
         * - DISBURSED(이미 프리랜서에게 지급 완료)된 회차는 취소 대상에서 제외됩니다.
         * - 본인 계약이 아닌 contractId로 요청 시 403 반환합니다.
         * - 취소할 PENDING 회차가 없으면 200으로 응답하되 아무 처리도 하지 않습니다.
         * """)
         * 
         * @PostMapping("/cancel-refund")
         * public ResponseEntity<ApiResponse<Void>> cancelAndRefund(
         * 
         * @RequestParam Long contractId,
         * 
         * @RequestParam(defaultValue = "계약 취소") String reason,
         * 
         * @AuthenticationPrincipal CustomUserDetails user) {
         * 
         * employerSettlementService.cancelAndRefund(contractId, user.getId(), reason);
         * ApiResponse<Void> apiResponse = ApiResponse.ok(null);
         * return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
         * }
         */
}
