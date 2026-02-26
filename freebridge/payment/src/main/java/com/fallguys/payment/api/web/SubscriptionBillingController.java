package com.fallguys.payment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.SubscriptionBillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Subscription Billing", description = "구독 결제 내역 API (Payment 모듈 소유)")
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionBillingController {

    private final SubscriptionBillingService subscriptionBillingService;

    // TODO: 로그인 기능 구현 후 @Authenticated로 변경
    @Operation(summary = "구독 결제 내역 조회",
            description = "인증된 고용주의 구독 업그레이드 결제 내역 페이지네이션 조회")
    @GetMapping("/billing-history")
    public ResponseEntity<ApiResponse<PageResponse<SubscriptionBillingItem>>> billingHistory(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<SubscriptionBillingItem> response =
                subscriptionBillingService.getBillingHistory(userId, status, page, size);
        ApiResponse<PageResponse<SubscriptionBillingItem>> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
