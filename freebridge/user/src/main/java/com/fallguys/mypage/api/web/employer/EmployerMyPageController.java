package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.dto.EmployerProfileDto;
import com.fallguys.mypage.dto.FreelancerProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class EmployerMyPageController {

    // TODO: 추후 Spring Security의 @AuthenticationPrincipal 등을 통해 실제 접속 유저 ID를 매핑해야 함.

    @GetMapping("/profile")
    public ApiResponse<EmployerProfileDto> getMyEmployerProfile(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(null);
    }

    @PutMapping("/profile")
    public ApiResponse<Void> updateMyEmployerProfile(@RequestHeader("X-User-Id") String userId,
                                                     @RequestBody EmployerProfileDto dto) {
        return ApiResponse.ok(null);
    }
}
