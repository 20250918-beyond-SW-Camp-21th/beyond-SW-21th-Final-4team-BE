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

    @Operation(summary = "구독 정보 조회", description = "현재 구독 플랜, 수수료율, 월 구독료 등을 반환합니다.\n(다운그레이드 예약 시 예약 정보도 추후 제공될 수 있습니다.)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "구독 정보 반환 성공")
    })
    @GetMapping
    public ApiResponse<SubscriptionResponse> getSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SubscriptionResponse response = subscriptionService.getSubscription(userDetails.getId());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "구독 플랜 변경", description = "구독 요금제(플랜)를 다른 등급으로 변경합니다.\n* 업그레이드(BASIC->PRO/PRIME): billingKey 필수, 즉시 결제 후 전환\n* 다운그레이드(PRIME->PRO): 결제 없이 다음 달 자동 전환 예약")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "플랜 변경(또는 예약) 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 플랜 값이거나 billingKey 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "402", description = "결제 실패")
    })
    @PutMapping
    public ApiResponse<Void> changePlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SubscriptionChangeRequest request) {
        subscriptionService.changePlan(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "구독 취소", description = "구독을 해지하고 BASIC 플랜으로 전환합니다. (선택적으로 취소 사유 입력 가능)")
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "구독 취소 및 BASIC 전환 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 BASIC 플랜 사용 중인 경우")
    })
    @DeleteMapping("/cancel")
    public ApiResponse<Void> cancelSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody(required = false) SubscriptionCancelRequest request) {
        subscriptionService.cancelSubscription(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }
}
