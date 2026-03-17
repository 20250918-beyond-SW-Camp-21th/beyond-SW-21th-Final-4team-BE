package com.fallguys.mypage.api.web.employer;


import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.api.web.dto.employer.request.UpdateNotificationSettingsRequestDto;
import com.fallguys.mypage.api.web.dto.employer.request.UpdatePasswordRequestDto;
import com.fallguys.mypage.api.web.dto.employer.request.UpdateSubscriptionRequestDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerNotificationSettingsDto;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerSubscriptionResponseDto;
import com.fallguys.subscription.api.response.SubscriptionChangeResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Tag(name = "4. Employer MyPage - Account", description = "고용주 마이페이지 계정 관리 API")
@RestController
@RequestMapping("/api/employer/mypage/account")
@RequiredArgsConstructor
public class EmployerAccountController {

    private final com.fallguys.mypage.service.employer.EmployerAccountService employerAccountService;

    @Operation(summary = "현재 구독 정보 조회", description = "현재 이용 중인 플랜 정보를 조회합니다.")
    @GetMapping("/subscription")
    public ApiResponse<EmployerSubscriptionResponseDto> getSubscription(@AuthenticationPrincipal CustomUserDetails userDetails) {
        EmployerSubscriptionResponseDto response = employerAccountService.getSubscription(userDetails.getId());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "구독 플랜 변경 신청", description = "플랜 변경을 요청합니다. 업그레이드는 billingKey가 필요하며, 다운그레이드는 즉시 반영됩니다.")
    @PutMapping("/subscription")
    public ApiResponse<SubscriptionChangeResultResponse> updateSubscription(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                            @Valid @RequestBody UpdateSubscriptionRequestDto request) {
        SubscriptionChangeResultResponse response = employerAccountService.updateSubscription(userDetails.getId(), request);
        return ApiResponse.ok(response);
    }

    @Operation(summary = "비밀번호 변경", description = "고용주 계정의 비밀번호를 변경합니다.")
    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @Valid @RequestBody UpdatePasswordRequestDto request) {
        employerAccountService.updatePassword(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "알림 설정 조회", description = "이메일 수신 동의 여부를 조회합니다.")
    @GetMapping("/notifications")
    public ApiResponse<EmployerNotificationSettingsDto> getNotificationSettings(@AuthenticationPrincipal CustomUserDetails userDetails) {
        EmployerNotificationSettingsDto response = employerAccountService.getNotificationSettings(userDetails.getId());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "이메일 알림 설정 변경", description = "이메일 수신 동의 여부를 변경합니다.")
    @PutMapping("/notifications")
    public ApiResponse<Void> updateNotificationSettings(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @Valid @RequestBody UpdateNotificationSettingsRequestDto request) {
        employerAccountService.updateNotificationSettings(userDetails.getId(), request.emailEnabled());
        return ApiResponse.ok(null);
    }
}
