package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.mypage.api.web.dto.resume.FreelancerResumeResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "8. Freelancer MyPage - Resume", description = "프리랜서 마이페이지 이력서(학력/경력) API")
@RestController
@RequestMapping("/api/freelancer/mypage/resume")
@RequiredArgsConstructor
public class FreelancerResumeController {

    @Operation(summary = "이력서 (학력/경력) 병합 조회", description = "현재 프리랜서의 모든 학력 및 경력 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<FreelancerResumeResponseDto> getResume(@RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(null);
    }
    
    // (참고) 학력/경력 CUD(Create, Update, Delete)는 각각 별도 엔드포인트(/educations, /careers)를 두거나 PUT 전체 덮어쓰기 방식으로 구현
}
