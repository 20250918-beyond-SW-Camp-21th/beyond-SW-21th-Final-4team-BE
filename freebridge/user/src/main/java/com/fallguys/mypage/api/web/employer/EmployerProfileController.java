package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.dto.EmployerProfileDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Employer MyPage - Profile", description = "고용주 마이페이지 프로필 관리 API")
@RestController
@RequestMapping("/api/mypage/employer/profile")
@RequiredArgsConstructor
public class EmployerProfileController {

    @Operation(summary = "고용주 프로필 조회", description = "고용주 대시보드 및 프로필 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<EmployerProfileDto> getProfile(@RequestHeader("X-User-Id") String userId) {
        // TODO: EmployerProfileDto 반환 로직 구현
        // Service: myPageService.getEmployerProfile(UUID.fromString(userId));
        return ApiResponse.ok(null);
    }

    @Operation(summary = "고용주 프로필 수정", description = "고용주 프로필 정보를 수정합니다.")
    @PutMapping
    public ApiResponse<Void> updateProfile(@RequestHeader("X-User-Id") String userId,
                                           @RequestBody EmployerProfileDto dto) {
        // TODO: 프로필 업데이트 로직 구현
        // Service: myPageService.updateEmployerProfile(UUID.fromString(userId), dto);
        return ApiResponse.ok(null);
    }
}
