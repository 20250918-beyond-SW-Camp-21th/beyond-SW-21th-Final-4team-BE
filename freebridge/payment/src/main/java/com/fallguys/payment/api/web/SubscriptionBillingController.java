package com.fallguys.payment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.SubscriptionBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.net.URI;

@Tag(name = "Subscription Billing", description = "구독 결제 내역 API (Payment 모듈 소유)")
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionBillingController {

        private final SubscriptionBillingService subscriptionBillingService;

        @Operation(summary = "구독 결제 내역 조회", description = "인증된 고용주의 구독 업그레이드 결제 내역 페이지네이션 조회")
        @PreAuthorize("hasRole('EMPLOYER')")
        @GetMapping("/billing-history")
        public ResponseEntity<ApiResponse<PageResponse<SubscriptionBillingItem>>> billingHistory(
                        @AuthenticationPrincipal CustomUserDetails user,
                        @RequestParam(defaultValue = "ALL") String status,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(defaultValue = "10") int size) {

                PageResponse<SubscriptionBillingItem> response = subscriptionBillingService
                                .getBillingHistory(user.getId(), status, page, size);
                ApiResponse<PageResponse<SubscriptionBillingItem>> apiResponse = ApiResponse.ok(response);
                return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
        }

        @Operation(summary = "구독 인보이스 URL 조회", description = "구독 결제 인보이스 PDF URL 반환")
        @PreAuthorize("hasRole('EMPLOYER')")
        @GetMapping("/billing/{billingId}/invoice")
        public ResponseEntity<ApiResponse<String>> invoiceUrl(
                        @AuthenticationPrincipal CustomUserDetails user,
                        @PathVariable Long billingId) {

                String pdfUrl = subscriptionBillingService.getInvoicePdfUrl(user.getId(), billingId);
                ApiResponse<String> apiResponse = ApiResponse.ok(pdfUrl);
                return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
        }

        @Operation(summary = "구독 인보이스 다운로드 (redirect)", description = "저장된 PDF URL로 리다이렉트합니다.")
        @PreAuthorize("hasRole('EMPLOYER')")
        @GetMapping("/billing/{billingId}/invoice/download")
        public ResponseEntity<Void> downloadInvoice(
                        @AuthenticationPrincipal CustomUserDetails user,
                        @PathVariable Long billingId) {

                String pdfUrl = subscriptionBillingService.getInvoicePdfUrl(user.getId(), billingId);
                if (pdfUrl == null || pdfUrl.isBlank()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
                }
                return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(pdfUrl)).build();
        }
}
