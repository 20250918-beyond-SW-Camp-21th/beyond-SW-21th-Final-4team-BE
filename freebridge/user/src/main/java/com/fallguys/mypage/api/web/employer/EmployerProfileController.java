package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.api.web.dto.employer.request.EmployerProfileUpdateRequestDto;
import com.fallguys.mypage.api.web.dto.employer.response.CrmAlertsResponseDto;
import com.fallguys.mypage.api.web.dto.employer.response.EmployerBasicProfileDto;
import com.fallguys.mypage.service.employer.EmployerProfileService;
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

    EmployerProfileService employerProfileService;

    @Operation(summary = "고용주 프로필 조회", description = "고용주 프로필 정보 조회합니다.")
    @GetMapping
    public ApiResponse<EmployerBasicProfileDto> getProfile(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(employerProfileService.getEmployerProfile(userId));
    }

    @Operation(summary = "고용주 CRM 마케팅 알림 조회", description = "프리미엄 요금제 업셀링 대상 여부 등을 조회합니다.")
    @GetMapping("/crm-alerts")
    public ApiResponse<CrmAlertsResponseDto> getCrmAlerts(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(employerProfileService.getCrmAlerts(userId));
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
