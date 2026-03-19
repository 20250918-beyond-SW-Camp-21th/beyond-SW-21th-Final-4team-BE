package com.fallguys.recruitment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.AiRecommendationResponseDTO;
import com.fallguys.recruitment.api.dto.response.EmployerProjectSearchDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.MatchedFreelancerResponseDTO;
import com.fallguys.recruitment.api.dto.response.PagedResponseDTO;
import com.fallguys.recruitment.api.support.TokenUserIdResolver;
import com.fallguys.recruitment.api.util.PagingUtils;
import com.fallguys.recruitment.service.JobPostingService;
import com.fallguys.recruitment.service.support.RecommendationPendingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Recruitment - Employer", description = "고용주 채용 공고 관리 API")
public class JobPostingEmployerController {

    private final JobPostingService jobPostingService;
    private final TokenUserIdResolver tokenUserIdResolver;

    @Operation(summary = "내 채용 공고 목록 조회", description = "고용주가 등록한 채용 공고 목록을 조회합니다.")
    @GetMapping("/api/employer/jobs")
    public ResponseEntity<ApiResponse<PagedResponseDTO<JobPostingSearchDTO>>> getMyJobPostings(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = tokenUserIdResolver.resolveUserId(authorization);
        List<JobPostingSearchDTO> result = jobPostingService.getJobPostings(userId);
        return ResponseEntity.ok(ApiResponse.ok(PagingUtils.toPagedResponse(result, page, size)));
    }

    @Operation(summary = "고용주: 자신의 프로젝트 조회", description = "고용주의 프로젝트 목록을 조회합니다.")
    @GetMapping("/api/employer/project")
    public ResponseEntity<ApiResponse<PagedResponseDTO<EmployerProjectSearchDTO>>> getMyProjects(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = tokenUserIdResolver.resolveUserId(authorization);
        List<EmployerProjectSearchDTO> result = jobPostingService.getEmployerProjects(userId);
        return ResponseEntity.ok(ApiResponse.ok(PagingUtils.toPagedResponse(result, page, size)));
    }

    @Operation(summary = "프로젝트 매칭 프리랜서 목록 조회", description = "프로젝트와 연결된 공고 기준으로 매칭된 프리랜서 목록을 조회합니다.")
    @GetMapping("/api/employer/projects/{projectId}/matched-freelancers")
    public ResponseEntity<ApiResponse<PagedResponseDTO<MatchedFreelancerResponseDTO>>> getMatchedFreelancers(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        Long userId = tokenUserIdResolver.resolveUserId(authorization);
        Page<MatchedFreelancerResponseDTO> result = jobPostingService.getMatchedFreelancers(
                projectId,
                userId,
                PageRequest.of(safePage, safeSize)
        );
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponseDTO<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        )));
    }

    @Operation(summary = "채용 공고 등록", description = "새로운 채용 공고를 등록합니다.")
    @PostMapping("/api/employer/jobs/post")
    public ResponseEntity<ApiResponse<Void>> createJobPosting(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody JobPostingCreateDTO body
    ) {
        Long userId = tokenUserIdResolver.resolveUserId(authorization);
        jobPostingService.createJobPosting(body, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "채용 공고 수정", description = "기존 채용 공고 내용을 수정합니다.")
    @PutMapping("/api/employer/jobs/put")
    public ResponseEntity<ApiResponse<Void>> updateJobPosting(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "jobsNumber") Long jobsNumber,
            @RequestBody JobPostingUpdateDTO body
    ) {
        Long userId = tokenUserIdResolver.resolveUserId(authorization);
        jobPostingService.updateJobPosting(body, jobsNumber, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "채용 공고 삭제", description = "등록된 채용 공고를 삭제합니다.")
    @DeleteMapping("/api/employer/jobs/del")
    public ResponseEntity<ApiResponse<Void>> deleteJobPosting(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(name = "jobsNumber") Long jobsNumber
    ) {
        Long userId = tokenUserIdResolver.resolveUserId(authorization);
        jobPostingService.deleteJobPosting(jobsNumber, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "공고 맞춤 프리랜서 추천", description = "공고 내용을 분석하여 적합한 프리랜서 7명을 추천합니다.")
    @GetMapping("/api/v1/employer/jobs/{jobPostingId}/recommendations")
    public ResponseEntity<ApiResponse<List<AiRecommendationResponseDTO>>> getFreelancerRecommendations(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long jobPostingId
    ) {
        Long userId = tokenUserIdResolver.resolveUserId(authorization);

        try {
            List<AiRecommendationResponseDTO> recommendations =
                    jobPostingService.getRecommendedFreelancers(jobPostingId, userId);
            return ResponseEntity.ok(ApiResponse.ok(recommendations));
        } catch (RecommendationPendingException ignored) {
            return ResponseEntity.accepted()
                    .header("Retry-After", "3")
                    .body(ApiResponse.ok(List.of()));
        }
    }

    @Operation(summary = "프로젝트 완료 처리", description = "고용주가 프로젝트를 완료 처리하고 해당 내용을 AI 서버에 동기화합니다.")
    @PostMapping("/api/v1/employer/projects/{projectId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeProject(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long projectId
    ) {
        // 1. 토큰에서 유저 ID 추출
        Long userId = tokenUserIdResolver.resolveUserId(authorization);

        // 2. 서비스의 completeProject 호출
        jobPostingService.completeProject(projectId, userId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
