package com.fallguys.recruitment.api;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.recruitment.api.support.TokenUserIdResolver;
import com.fallguys.recruitment.api.dto.response.FreelancerJobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.PagedResponseDTO;
import com.fallguys.recruitment.api.util.PagingUtils;
import com.fallguys.recruitment.service.JobPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Recruitment - Freelancer", description = "프리랜서 채용 공고 조회 및 관심 공고 관리 API")
public class JobPostingFreelancerController {

    private final JobPostingService jobPostingService;
    private final TokenUserIdResolver tokenUserIdResolver;

    @Operation(summary = "채용 공고 검색", description = "프리랜서가 조건에 맞는 채용 공고를 조회합니다.")
    @GetMapping("/api/v1/freelancer/jobs")
    public ResponseEntity<ApiResponse<PagedResponseDTO<FreelancerJobPostingSearchDTO>>> searchJobPostings(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "liked", defaultValue = "false") boolean liked,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = tokenUserIdResolver.resolveUserId(authorization);
        List<FreelancerJobPostingSearchDTO> result =
                jobPostingService.searchJobPostingsForFreelancer(userId, keyword, liked);
        PagedResponseDTO<FreelancerJobPostingSearchDTO> paged = PagingUtils.toPagedResponse(result, page, size);

        return ResponseEntity.ok(ApiResponse.ok(paged));
    }

    @Operation(summary = "관심 공고 등록", description = "채용 공고를 관심 목록에 추가합니다.")
    @PostMapping("/api/v1/freelancer/jobs/{jobPostingId}/like")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long jobPostingId
    ) {
        Long userId = tokenUserIdResolver.resolveUserId(authorization);
        jobPostingService.addFavoriteJobPosting(userId, jobPostingId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "관심 공고 해제", description = "관심 목록에서 채용 공고를 제거합니다.")
    @DeleteMapping("/api/v1/freelancer/jobs/{jobPostingId}/like")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long jobPostingId
    ) {
        Long userId = tokenUserIdResolver.resolveUserId(authorization);
        jobPostingService.removeFavoriteJobPosting(userId, jobPostingId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
