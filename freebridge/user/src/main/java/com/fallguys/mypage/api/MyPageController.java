package com.fallguys.mypage.api;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.dto.EmployerProfileDto;
import com.fallguys.mypage.dto.FreelancerProfileDto;
import com.fallguys.mypage.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    // TODO: 추후 Spring Security의 @AuthenticationPrincipal 등을 통해 실제 접속 유저 ID를 매핑해야 함.
    // 현재는 API 테스트를 위해 헤더나 파라미터로 ID를 받거나 임시 ID를 사용해야 하지만,
    // 명세에 따라 내 정보 조회(/me) 컨셉을 유지하되, 임시로 RequestHeader에서 X-User-Id를 꺼내 사용하도록 구현.
    
    @GetMapping("/freelancer/me")
    public ApiResponse<FreelancerProfileDto> getMyFreelancerProfile(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(myPageService.getFreelancerProfile(UUID.fromString(userId)));
    }

    @PutMapping("/freelancer/me")
    public ApiResponse<Void> updateMyFreelancerProfile(@RequestHeader("X-User-Id") String userId,
                                                       @RequestBody FreelancerProfileDto dto) {
        myPageService.updateFreelancerProfile(UUID.fromString(userId), dto);
        return ApiResponse.ok(null);
    }

    @GetMapping("/employer/me")
    public ApiResponse<EmployerProfileDto> getMyEmployerProfile(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(myPageService.getEmployerProfile(UUID.fromString(userId)));
    }

    @PutMapping("/employer/me")
    public ApiResponse<Void> updateMyEmployerProfile(@RequestHeader("X-User-Id") String userId,
                                                     @RequestBody EmployerProfileDto dto) {
        myPageService.updateEmployerProfile(UUID.fromString(userId), dto);
        return ApiResponse.ok(null);
    }
}
