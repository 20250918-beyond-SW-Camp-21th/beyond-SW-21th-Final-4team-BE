package com.fallguys.recruitment.api;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.recruitment.api.dto.request.JobPostingCreateDTO;
import com.fallguys.recruitment.api.dto.request.JobPostingUpdateDTO;
import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.api.dto.response.PagedResponseDTO;
import com.fallguys.recruitment.api.util.PagingUtils;
import com.fallguys.recruitment.service.JobPostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class JobPostingEmployerController {

    private final JobPostingService jobPostingService;

    @GetMapping("/api/v1/employer/jobs")
    public ResponseEntity<ApiResponse<PagedResponseDTO<JobPostingSearchDTO>>> getMyJobPostings(
            @RequestParam Long employerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<JobPostingSearchDTO> result = jobPostingService.getJobPostings(employerId);
        return ResponseEntity.ok(ApiResponse.ok(PagingUtils.toPagedResponse(result, page, size)));
    }

    @PostMapping("/api/v1/employer/jobs/post")
    public ResponseEntity<ApiResponse<Void>> createJobPosting(
            @RequestParam Long employerId,
            @RequestBody JobPostingCreateDTO request
    ) {
        jobPostingService.createJobPosting(request, employerId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/api/v1/employer/jobs/put")
    public ResponseEntity<ApiResponse<Void>> updateJobPosting(
            @RequestParam(name = "jobsNumber") Long jobsNumber,
            @RequestParam Long employerId,
            @RequestBody JobPostingUpdateDTO request
    ) {
        jobPostingService.updateJobPosting(request, jobsNumber, employerId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/api/v1/employer/jobs/del")
    public ResponseEntity<ApiResponse<Void>> deleteJobPosting(
            @RequestParam(name = "jobsNumber") Long jobsNumber,
            @RequestParam Long employerId
    ) {
        jobPostingService.deleteJobPosting(jobsNumber, employerId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
