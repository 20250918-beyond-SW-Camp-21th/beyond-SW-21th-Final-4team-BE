package com.fallguys.mypage.api.web.employer;

import com.fallguys.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Employer MyPage - Checklist", description = "고용주 관심 프리랜서(체크리스트) API")
@RestController
@RequestMapping("/api/mypage/employer/checklist")
@RequiredArgsConstructor
public class EmployerChecklistController {

    @Operation(summary = "관심 프리랜서 목록 조회", description = "고용주가 찜(체크)한 프리랜서 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getChecklist(@RequestHeader("X-User-Id") String userId) {
        // TODO: 체크리스트 조회 로직 구현
        return ApiResponse.ok(null);
    }
}
