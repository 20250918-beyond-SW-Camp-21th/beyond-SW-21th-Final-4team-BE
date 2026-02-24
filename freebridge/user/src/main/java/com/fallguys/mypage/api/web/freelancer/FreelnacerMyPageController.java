package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.dto.EmployerProfileDto;
import com.fallguys.mypage.dto.FreelancerProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class FreelnacerMyPageController {

    // TODO: 추후 Spring Security의 @AuthenticationPrincipal 등을 통해 실제 접속 유저 ID를 매핑해야 함.
    
    @GetMapping("/freelancer/me")
    public ApiResponse<FreelancerProfileDto> getMyFreelancerProfile(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(null);
    }

    @PutMapping("/freelancer/me")
    public ApiResponse<Void> updateMyFreelancerProfile(@RequestHeader("X-User-Id") String userId,
                                                       @RequestBody FreelancerProfileDto dto) {
        return ApiResponse.ok(null);
    }

    @GetMapping("/employer/me")
    public ApiResponse<EmployerProfileDto> getMyEmployerProfile(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(null);
    }

    @PutMapping("/employer/me")
    public ApiResponse<Void> updateMyEmployerProfile(@RequestHeader("X-User-Id") String userId,
                                                     @RequestBody EmployerProfileDto dto) {
        return ApiResponse.ok(null);
    }
}
