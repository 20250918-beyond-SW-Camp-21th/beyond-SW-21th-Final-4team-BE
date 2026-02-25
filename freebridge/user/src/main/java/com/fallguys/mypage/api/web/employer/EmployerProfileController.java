package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.dto.employer.request.EmployerProfileUpdateRequestDto;
import com.fallguys.mypage.dto.employer.response.EmployerProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Employer MyPage - Profile", description = "고용주 마이페이지 프로필 관리 API")
@RestController
@RequestMapping("/api/employer/mypage/profile")
@RequiredArgsConstructor
public class EmployerProfileController {

    @Operation(summary = "고용주 프로필 조회", description = "고용주 프로필 정보(평점, 프로젝트 현황 포함)를 조회합니다.")
    @GetMapping
    public ApiResponse<EmployerProfileResponse> getProfile(@RequestHeader("X-User-Id") String userId) {
        // TODO: myPageService.getEmployerProfile(userId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "고용주 프로필 수정", description = "고용주 프로필 정보를 수정합니다.")
    @PutMapping
    public ApiResponse<Void> updateProfile(@RequestHeader("X-User-Id") String userId,
                                           @RequestBody EmployerProfileUpdateRequestDto request) {
        // TODO: myPageService.updateEmployerProfile(userId, request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "로고 이미지 수정", description = "고용주 로고 이미지를 업로드하고 반환합니다.")
    @PostMapping("/logo")
    public ApiResponse<String> uploadLogo(@RequestHeader("X-User-Id") String userId,
                                          @RequestPart("file") MultipartFile file) {
        // TODO: s3Service.upload(file) ...
        return ApiResponse.ok("image_url_string");
    }
}
