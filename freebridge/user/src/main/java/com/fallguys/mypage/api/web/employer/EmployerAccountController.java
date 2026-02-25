package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.dto.employer.response.EmployerNotificationSettingsDto;
import com.fallguys.mypage.dto.employer.request.UpdatePasswordRequestDto;
import com.fallguys.mypage.dto.employer.request.UpdateSubscriptionRequestDto;
import com.fallguys.mypage.dto.employer.response.EmployerSubscriptionResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Employer MyPage - Account", description = "고용주 마이페이지 계정 관리 API")
@RestController
@RequestMapping("/api/employer/mypage/account")
@RequiredArgsConstructor
public class EmployerAccountController {

    @Operation(summary = "현재 구독 정보 조회", description = "현재 이용 중인 플랜 정보를 조회합니다.")
    @GetMapping("/subscription")
    public ApiResponse<EmployerSubscriptionResponseDto> getSubscription(@RequestHeader("X-User-Id") String userId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }

    @Operation(summary = "구독 플랜 변경 신청", description = "프라임 멤버십 등 다른 플랜으로 변경을 요청합니다.")
    @PutMapping("/subscription")
    public ApiResponse<Void> updateSubscription(@RequestHeader("X-User-Id") String userId,
                                                @RequestBody UpdateSubscriptionRequestDto request) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }

    @Operation(summary = "비밀번호 변경", description = "고용주 계정의 비밀번호를 변경합니다.")
    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@RequestHeader("X-User-Id") String userId,
                                            @RequestBody UpdatePasswordRequestDto request) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }

    @Operation(summary = "알림 설정 조회", description = "이메일/카톡 알림 수신 동의 여부를 조회합니다.")
    @GetMapping("/notifications")
    public ApiResponse<EmployerNotificationSettingsDto> getNotificationSettings(@RequestHeader("X-User-Id") String userId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }

    @Operation(summary = "알림 설정 변경", description = "알림 수신 동의 여부를 변경합니다.")
    @PutMapping("/notifications")
    public ApiResponse<Void> updateNotificationSettings(@RequestHeader("X-User-Id") String userId,
                                                        @RequestBody EmployerNotificationSettingsDto request) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
