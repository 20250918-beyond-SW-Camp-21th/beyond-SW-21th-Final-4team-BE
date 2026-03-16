package com.fallguys.subscription.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.subscription.api.request.SubscriptionChangeRequest;
import com.fallguys.subscription.api.response.SubscriptionChangeResultResponse;
import com.fallguys.subscription.api.response.SubscriptionResponse;
import com.fallguys.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 고용주 Employer 구독 관리 REST Controller.
 * <p>
 * 구독 조회, 플랜 변경 API를 제공한다.
 * 인증은 JWT를 통해 처리하고, {@link CustomUserDetails}에서 userId를 추출한다.
 */
@Tag(name = "Employer Subscription", description = "고용주 구독 플랜 관리 API")
@RestController
@RequestMapping("/api/employer/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "구독 정보 조회", description = "현재 구독 플랜, 수수료율, 월 구독료 등을 반환한다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "구독 정보 반환 성공")
    })
    @GetMapping
    public ApiResponse<SubscriptionResponse> getSubscription(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        SubscriptionResponse response = subscriptionService.getSubscription(userDetails.getId());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "구독 플랜 변경", description = "플랜 변경 요청을 처리한다.\n* 업그레이드(BASIC->PRO/PRIME, PRO->PRIME): billingKey 필수, 결제 성공 시 즉시 반영\n* 다운그레이드(PRIME->PRO, PRO/PRIME->BASIC): 경고 확인 후 즉시 반영")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "플랜 변경 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 플랜 값이거나 billingKey 누락"),
    })
    @PutMapping
    public ApiResponse<SubscriptionChangeResultResponse> changePlan(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody SubscriptionChangeRequest request) {
        SubscriptionChangeResultResponse response = subscriptionService.changePlan(userDetails.getId(), request);
        return ApiResponse.ok(response);
    }
}
