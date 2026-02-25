package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.dto.employer.request.UpdatePasswordRequestDto;
import com.fallguys.mypage.dto.freelancer.response.FreelancerNotificationSettingsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Freelancer MyPage - Account", description = "프리랜서 마이페이지 계정 관리 API")
@RestController
@RequestMapping("/api/freelancer/mypage/account")
@RequiredArgsConstructor
public class FreelancerAccountController {

    @Operation(summary = "알림 설정 조회", description = "프리랜서 알림 수신 여부를 조회합니다.")
    @GetMapping("/notifications")
    public ApiResponse<FreelancerNotificationSettingsDto> getNotifications(@RequestHeader("X-User-Id") String userId) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }

    @Operation(summary = "알림 설정 수정", description = "프리랜서 알림 수신 여부를 변경합니다.")
    @PutMapping("/notifications")
    public ApiResponse<Void> updateNotifications(@RequestHeader("X-User-Id") String userId,
                                                 @RequestBody FreelancerNotificationSettingsDto request) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }

    @Operation(summary = "비밀번호 변경", description = "해당 계정의 비밀번호를 안전하게 변경합니다.")
    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(@RequestHeader("X-User-Id") String userId,
                                            @RequestBody UpdatePasswordRequestDto request) { // 고용주 측 DTO 재사용 가능
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented yet");
    }
}
