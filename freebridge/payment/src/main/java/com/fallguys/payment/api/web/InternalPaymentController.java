package com.fallguys.payment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.SubscriptionPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 구독 결제 처리 API (프론트엔드 → 백엔드 직접 호출)
 *
 * [테스트 모드 결제 흐름]
 * 1. 프론트: PortOne SDK로 빌링키 발급 (테스트 카드: 4111 1111 1111 1111 / 유효기간: 임의 미래 / CVC: 임의)
 * 2. 프론트: 발급된 billingKey + planType을 아래 /subscription 엔드포인트로 전송
 * 3. 백엔드: PortOne 테스트 API로 즉시 결제 → BillingKey DB 저장 → PLATFORM_REVENUE 지갑 크레딧
 * 4. 이후 스케줄러가 매월 1일 저장된 빌링키로 자동 재결제 (chargeScheduled 사용)
 */
@Tag(name = "Internal Payment", description = "구독 결제 처리 API")
@RestController
@RequestMapping("/api/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final SubscriptionPaymentService subscriptionPaymentService;

    @Operation(summary = "구독 결제 처리",
            description = """
                    프론트엔드에서 PortOne SDK로 빌링키를 발급받은 후 호출합니다.

                    [테스트 모드]
                    - 테스트 카드 번호: 4111 1111 1111 1111 (VISA)
                    - 유효기간: 임의의 미래 날짜 (예: 12/26)
                    - CVC: 임의 3자리
                    - 실제 결제 없이 PortOne 테스트 서버에서 처리됩니다.

                    성공 시 BillingKey가 DB에 저장되고 매월 1일 자동 재결제됩니다.
                    실패 시 구독 플랜 변경을 롤백해야 합니다.
                    """)
    @PostMapping("/subscription")
    public ResponseEntity<ApiResponse<SubscriptionPaymentResponse>> processSubscriptionPayment(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody SubscriptionPaymentRequest request) {

        // employerId는 클라이언트 바디가 아닌 인증 토큰에서 추출 (임의 위조 방지)
        SubscriptionPaymentRequest secureRequest = new SubscriptionPaymentRequest(
                user.getId(), request.getPlanType(), request.getAmount(), request.getBillingKey(), request.getPaymentId());
        SubscriptionPaymentResponse response = subscriptionPaymentService.processPayment(secureRequest);
        ApiResponse<SubscriptionPaymentResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "[Internal] 구독 결제 내역 단건 조회",
            description = "billingId로 SubscriptionBilling 레코드 조회. 본인 결제 내역만 조회 가능합니다.")
    @GetMapping("/subscription/{billingId}")
    public ResponseEntity<ApiResponse<SubscriptionBillingItem>> getSubscriptionBilling(
            @PathVariable Long billingId,
            @AuthenticationPrincipal CustomUserDetails user) {

        SubscriptionBillingItem response = subscriptionPaymentService.getBillingById(billingId, user.getId());
        ApiResponse<SubscriptionBillingItem> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
