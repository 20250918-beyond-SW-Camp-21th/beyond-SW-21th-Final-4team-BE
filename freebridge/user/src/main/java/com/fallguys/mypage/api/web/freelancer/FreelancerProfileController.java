package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.mypage.api.web.dto.freelancer.request.FreelancerProfileUpdateRequestDto;
import com.fallguys.mypage.api.web.dto.freelancer.response.FreelancerProfileResponseDto;
import com.fallguys.mypage.service.freelancer.FreelancerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "6. Freelancer MyPage - Profile", description = "프리랜서 마이페이지 프로필 관련 API")
@RestController
@RequestMapping("/api/freelancer/mypage/profile")
@RequiredArgsConstructor
public class FreelancerProfileController {

    private final FreelancerProfileService freelancerProfileService;

    @Operation(summary = "프리랜서 프로필 조회", description = "대시보드에 노출될 프로필 기본 정보와 통계를 조회합니다.")
    @GetMapping
    public ApiResponse<FreelancerProfileResponseDto> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(freelancerProfileService.getProfile((userDetails.getId())));
    }

    @Operation(summary = "프리랜서 프로필 수정", description = "직무, 소개, 스킬 등 프로필 정보를 수정합니다.")
    @PutMapping
    public ApiResponse<Void> updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @RequestBody FreelancerProfileUpdateRequestDto request) {
        freelancerProfileService.updateProfile((userDetails.getId()), request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "프로필 아바타 이미지 변경", description = "프리랜서의 프로필 이미지를 S3에 업로드하고 변경합니다.")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<String> uploadAvatar(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(freelancerProfileService.updateAvatarUrl((userDetails.getId()), file));
    }
}
