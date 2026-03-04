package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.mypage.api.web.dto.resume.FreelancerResumeResponseDto;
import com.fallguys.mypage.api.web.dto.resume.request.CareerRequestDto;
import com.fallguys.mypage.api.web.dto.resume.request.CertificationRequestDto;
import com.fallguys.mypage.api.web.dto.resume.request.EducationRequestDto;
import com.fallguys.mypage.api.web.dto.resume.request.ResumeBasicInfoRequestDto;
import com.fallguys.mypage.service.freelancer.FreelancerResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "8. Freelancer MyPage - Resume", description = "프리랜서 마이페이지 이력서(학력/경력) API")
@RestController
@RequestMapping("/api/freelancer/mypage/resume")
@RequiredArgsConstructor
public class FreelancerResumeController {

    private final FreelancerResumeService freelancerResumeService;

    // ─── 이력서 조회 ──────────────────────────────────────────────

    @Operation(summary = "이력서 조회", description = "현재 프리랜서의 모든 학력, 경력, 자격증 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<FreelancerResumeResponseDto> getResume(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.ok(freelancerResumeService.getResume(userDetails.getId()));
    }

    // ─── 이력서 기본정보 수정 ──────────────────────────────────────

    @Operation(summary = "이력서 기본정보 수정", description = "이름, 생년월일, 연락처 등 기본 인적사항을 수정합니다.")
    @PutMapping
    public ApiResponse<Void> updateResumeBasicInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                   @RequestBody ResumeBasicInfoRequestDto request) {
        freelancerResumeService.updateResumeBasicInfo(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }

    // ─── 학력 CUD ─────────────────────────────────────────────────

    @Operation(summary = "학력 추가", description = "이력서에 학력 항목을 추가합니다.")
    @PostMapping("/educations")
    public ApiResponse<Void> addEducation(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @RequestBody EducationRequestDto request) {
        freelancerResumeService.addEducation(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "학력 수정", description = "인덱스(0-based)로 학력 항목을 수정합니다.")
    @PutMapping("/educations/{index}")
    public ApiResponse<Void> updateEducation(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             @PathVariable int index,
                                             @RequestBody EducationRequestDto request) {
        freelancerResumeService.updateEducation(userDetails.getId(), index, request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "학력 삭제", description = "인덱스(0-based)로 학력 항목을 삭제합니다.")
    @DeleteMapping("/educations/{index}")
    public ApiResponse<Void> deleteEducation(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             @PathVariable int index) {
        freelancerResumeService.deleteEducation(userDetails.getId(), index);
        return ApiResponse.ok(null);
    }

    // ─── 경력 CUD ─────────────────────────────────────────────────

    @Operation(summary = "경력 추가", description = "이력서에 경력 항목을 추가합니다.")
    @PostMapping("/careers")
    public ApiResponse<Void> addCareer(@AuthenticationPrincipal CustomUserDetails userDetails,
                                       @RequestBody CareerRequestDto request) {
        freelancerResumeService.addCareer(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "경력 수정", description = "인덱스(0-based)로 경력 항목을 수정합니다.")
    @PutMapping("/careers/{index}")
    public ApiResponse<Void> updateCareer(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @PathVariable int index,
                                          @RequestBody CareerRequestDto request) {
        freelancerResumeService.updateCareer(userDetails.getId(), index, request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "경력 삭제", description = "인덱스(0-based)로 경력 항목을 삭제합니다.")
    @DeleteMapping("/careers/{index}")
    public ApiResponse<Void> deleteCareer(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @PathVariable int index) {
        freelancerResumeService.deleteCareer(userDetails.getId(), index);
        return ApiResponse.ok(null);
    }

    // ─── 자격증 CUD ───────────────────────────────────────────────

    @Operation(summary = "자격증 추가", description = "이력서에 자격증 항목을 추가합니다.")
    @PostMapping("/certifications")
    public ApiResponse<Void> addCertification(@AuthenticationPrincipal CustomUserDetails userDetails,
                                              @RequestBody CertificationRequestDto request) {
        freelancerResumeService.addCertification(userDetails.getId(), request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "자격증 수정", description = "인덱스(0-based)로 자격증 항목을 수정합니다.")
    @PutMapping("/certifications/{index}")
    public ApiResponse<Void> updateCertification(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @PathVariable int index,
                                                 @RequestBody CertificationRequestDto request) {
        freelancerResumeService.updateCertification(userDetails.getId(), index, request);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "자격증 삭제", description = "인덱스(0-based)로 자격증 항목을 삭제합니다.")
    @DeleteMapping("/certifications/{index}")
    public ApiResponse<Void> deleteCertification(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                 @PathVariable int index) {
        freelancerResumeService.deleteCertification(userDetails.getId(), index);
        return ApiResponse.ok(null);
    }
}
