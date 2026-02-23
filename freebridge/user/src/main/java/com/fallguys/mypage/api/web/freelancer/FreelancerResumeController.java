package com.fallguys.mypage.api.web.freelancer;

import com.fallguys.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Freelancer MyPage - Resume", description = "프리랜서 마이페이지 이력서 관리 API")
@RestController
@RequestMapping("/api/mypage/freelancer/resume")
@RequiredArgsConstructor
public class FreelancerResumeController {

    @Operation(summary = "이력서 상세 조회", description = "학력, 경력, 자격증 등 이력서 상세 정보를 조회합니다.")
    @GetMapping
    public ApiResponse<Map<String, Object>> getResumeDetail(@RequestHeader("X-User-Id") String userId) {
        // TODO: ResumeDetail DTO 정의 및 반환 로직 구현
        // ResumeDetail { educations: [], careers: [], certifications: [] }
        return ApiResponse.ok(null);
    }

    @Operation(summary = "이력서 상세 저장", description = "학력, 경력, 자격증 등 이력서 상세 정보를 저장합니다.")
    @PutMapping
    public ApiResponse<Void> saveResumeDetail(@RequestHeader("X-User-Id") String userId,
                                              @RequestBody Map<String, Object> resumeDetail) {
        // TODO: ResumeDetail DTO 매핑 및 저장 로직 구현
        // Service: careerService.saveResume(userId, resumeDetail);
        return ApiResponse.ok(null);
    }
}
