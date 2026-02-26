package com.fallguys.recruitment.api;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.PagedResponseDTO;
import com.fallguys.recruitment.api.util.PagingUtils;
import com.fallguys.recruitment.service.JobPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Recruitment - Employer", description = "고용주 채용 공고 관리 API")
public class JobPostingEmployerController {

    private final JobPostingService jobPostingService;

    @Operation(summary = "내 채용 공고 목록 조회", description = "고용주가 등록한 채용 공고 목록을 조회합니다.")
    @GetMapping("/api/v1/employer/jobs")
    public ResponseEntity<ApiResponse<PagedResponseDTO<JobPostingSearchDTO>>> getMyJobPostings(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        String userEmail = getCurrentUserEmail(principal);
        List<JobPostingSearchDTO> result = jobPostingService.getJobPostings(userEmail);
        return ResponseEntity.ok(ApiResponse.ok(PagingUtils.toPagedResponse(result, page, size)));
    }

    @Operation(summary = "채용 공고 등록", description = "새로운 채용 공고를 등록합니다.")
    @PostMapping("/api/v1/employer/jobs/post")
    public ResponseEntity<ApiResponse<Void>> createJobPosting(
            Principal principal,
            @RequestBody JobPostingCreateDTO body
    ) {
        String userEmail = getCurrentUserEmail(principal);
        jobPostingService.createJobPosting(body, userEmail);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "채용 공고 수정", description = "기존 채용 공고 내용을 수정합니다.")
    @PutMapping("/api/v1/employer/jobs/put")
    public ResponseEntity<ApiResponse<Void>> updateJobPosting(
            Principal principal,
            @RequestParam(name = "jobsNumber") Long jobsNumber,
            @RequestBody JobPostingUpdateDTO body
    ) {
        String userEmail = getCurrentUserEmail(principal);
        jobPostingService.updateJobPosting(body, jobsNumber, userEmail);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "채용 공고 삭제", description = "등록된 채용 공고를 삭제합니다.")
    @DeleteMapping("/api/v1/employer/jobs/del")
    public ResponseEntity<ApiResponse<Void>> deleteJobPosting(
            Principal principal,
            @RequestParam(name = "jobsNumber") Long jobsNumber
    ) {
        String userEmail = getCurrentUserEmail(principal);
        jobPostingService.deleteJobPosting(jobsNumber, userEmail);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private String getCurrentUserEmail(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return principal.getName();
    }
}
