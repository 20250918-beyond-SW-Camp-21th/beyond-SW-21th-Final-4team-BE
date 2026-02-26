package com.fallguys.payment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.SubscriptionPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API consumed by the Subscription module.
 * Called when an employer upgrades their subscription plan (cheaper → more expensive).
 * The Subscription module sends the PortOne imp_uid after the employer completes payment on the frontend.
 */
@Tag(name = "Internal Payment", description = "내부 결제 처리 API (구독 모듈 전용)")
@RestController
@RequestMapping("/api/v1/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final SubscriptionPaymentService subscriptionPaymentService;

    @Operation(summary = "[Internal] 구독 업그레이드 결제 처리",
            description = """
                    구독 모듈이 호출하는 결제 처리 API.
                    PortOne imp_uid를 검증하고 SubscriptionBilling을 생성하며 PLATFORM_REVENUE 지갑에 크레딧합니다.
                    성공 시 구독 모듈이 planType을 업데이트하고, 실패 시 플랜 변경을 롤백해야 합니다.
                    멱등성 보장: 동일 imp_uid 재호출 시 기존 결과 반환.
                    """)
    @PostMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionPaymentResponse>> processSubscriptionPayment(
            @RequestBody SubscriptionPaymentRequest request) {

        SubscriptionPaymentResponse response = subscriptionPaymentService.processPayment(request);
        ApiResponse<SubscriptionPaymentResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "[Internal] 구독 결제 내역 단건 조회",
            description = "billingId로 SubscriptionBilling 레코드 조회")
    @GetMapping("/subscription/{billingId}")
    public ResponseEntity<ApiResponse<SubscriptionBillingItem>> getSubscriptionBilling(
            @PathVariable Long billingId) {

        SubscriptionBillingItem response = subscriptionPaymentService.getBillingById(billingId);
        ApiResponse<SubscriptionBillingItem> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
