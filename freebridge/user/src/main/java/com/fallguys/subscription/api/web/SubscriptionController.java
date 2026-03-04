package com.fallguys.subscription.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.subscription.api.request.SubscriptionCancelRequest;
import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionResponse;
import com.fallguys.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 고용주(Employer) 구독 관리 REST Controller.
 * <p>
 * 구독 조회, 플랜 변경, 구독 취소 API를 제공합니다.
 * 인증은 JWT를 통해 처리되며, {@link CustomUserDetails}에서 userId를 추출합니다.
 */
@Tag(name = "Employer Subscription", description = "고용주 구독 플랜 관리 API")
@RestController
@RequestMapping("/api/employer/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "구독 정보 조회", description = "현재 구독 플랜, 수수료율, 월 구독료를 조회합니다.")
    @GetMapping
    public ApiResponse<SubscriptionResponse> getSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SubscriptionResponse response = subscriptionService.getSubscription(userDetails.getId());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "구독 플랜 변경", description = "구독 요금제(플랜)를 다른 등급으로 변경합니다. (BASIC, PRO, PRIME)")
    @PutMapping
    public ApiResponse<Void> changePlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SubscriptionChangeRequest request) {
        subscriptionService.changePlan(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "구독 취소", description = "자동 결제를 취소합니다. 취소 시 즉시 BASIC 플랜으로 전환됩니다.")
    @DeleteMapping("/cancel")
    public ApiResponse<Void> cancelSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) SubscriptionCancelRequest request) {
        subscriptionService.cancelSubscription(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }
}
