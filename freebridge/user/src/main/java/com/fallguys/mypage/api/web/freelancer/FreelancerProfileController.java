package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.dto.FreelancerProfileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Freelancer MyPage - Profile", description = "프리랜서 마이페이지 프로필 관리 API")
@RestController
@RequestMapping("/api/mypage/freelancer/profile")
@RequiredArgsConstructor
public class FreelancerProfileController {

    @Operation(summary = "프리랜서 프로필 조회", description = "프리랜서 대시보드 및 프로필 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<FreelancerProfileDto> getProfile(@RequestHeader("X-User-Id") String userId) {
        // TODO: FreelancerProfileDto 반환 로직 구현
        // Service: myPageService.getFreelancerProfile(UUID.fromString(userId));
        return ApiResponse.ok(null);
    }

    @Operation(summary = "프리랜서 프로필 수정", description = "프리랜서 프로필 정보를 수정합니다.")
    @PutMapping
    public ApiResponse<Void> updateProfile(@RequestHeader("X-User-Id") String userId,
                                           @RequestBody FreelancerProfileDto dto) {
        // TODO: 프로필 업데이트 로직 구현
        // Service: myPageService.updateFreelancerProfile(UUID.fromString(userId), dto);
        return ApiResponse.ok(null);
    }
}
