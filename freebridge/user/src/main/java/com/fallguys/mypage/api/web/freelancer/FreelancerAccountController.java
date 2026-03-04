package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.mypage.api.web.dto.employer.request.UpdatePasswordRequestDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerNotificationSettingsDto;
import com.fallguys.mypage.service.freelancer.FreelancerAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@Tag(name = "5. Freelancer MyPage - Account", description = "프리랜서 마이페이지 계정 관리 API")
@RestController
@RequestMapping("/api/freelancer/mypage/account")
@RequiredArgsConstructor
public class FreelancerAccountController {

    private final FreelancerAccountService freelancerAccountService;

    @Operation(summary = "알림 설정 조회", description = "프리랜서 알림 수신 여부를 조회합니다.")
    @GetMapping("/notifications")
    public ApiResponse<FreelancerNotificationSettingsDto> getNotificationSettings(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(freelancerAccountService.getNotificationSettings(userDetails.getId()));
    }

    @Operation(summary = "알림 설정 수정", description = "프리랜서 알림 수신 여부를 변경합니다.")
    @PutMapping("/notifications")
    public ApiResponse<Void> updateNotificationSettings(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @Valid @RequestBody FreelancerNotificationSettingsDto request) {
        freelancerAccountService.updateNotificationSettings(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "비밀번호 변경", description = "해당 계정의 비밀번호를 안전하게 변경합니다.")
    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @Valid @RequestBody UpdatePasswordRequestDto request) {
        freelancerAccountService.updatePassword(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }
}
