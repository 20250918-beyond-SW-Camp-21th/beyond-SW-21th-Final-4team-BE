package com.fallguys.matchs.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.matchs.api.dto.request.ApplicationCreateRequest;
import com.fallguys.matchs.api.dto.request.ProposalCreateRequest;
import com.fallguys.matchs.api.dto.response.ApplicationResponseDTO;
import com.fallguys.matchs.api.dto.response.PagedResponseDTO;
import com.fallguys.matchs.api.dto.response.ProposalResponseDTO;
import com.fallguys.matchs.service.MatchsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MatchsController {

    private final MatchsService matchsService;

    @PostMapping("/api/v1/freelancer/application")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createApplication(
            @RequestParam Long freelancerId,
            @RequestBody ApplicationCreateRequest request
    ) {
        Long applicationId = matchsService.createApplication(freelancerId, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("applicationId", applicationId)));
    }

    @PostMapping("/api/v1/employer/proposals")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createProposal(
            @RequestParam Long employerId,
            @RequestBody ProposalCreateRequest request
    ) {
        Long proposalId = matchsService.createProposal(employerId, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("proposalId", proposalId)));
    }

    @GetMapping("/api/v1/employer/proposals/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalResponseDTO>> getEmployerProposal(
            @PathVariable Long proposalId,
            @RequestParam Long employerId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(matchsService.getEmployerProposal(employerId, proposalId)));
    }

    @GetMapping("/api/v1/employer/applications")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ApplicationResponseDTO>>> getEmployerApplications(
            @RequestParam Long employerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<ApplicationResponseDTO> result = matchsService.getEmployerApplications(employerId);
        return ResponseEntity.ok(ApiResponse.ok(toPagedResponse(result, page, size)));
    }

    @GetMapping("/api/v1/employer/applications/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> getEmployerApplication(
            @PathVariable Long applicationId,
            @RequestParam Long employerId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(matchsService.getEmployerApplication(employerId, applicationId)));
    }

    @PatchMapping("/api/v1/employer/agree/{applicationId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> acceptApplication(
            @PathVariable Long applicationId,
            @RequestParam Long employerId
    ) {
        Long projectId = matchsService.acceptApplication(employerId, applicationId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("projectId", projectId)));
    }

    @PatchMapping("/api/v1/employer/deny/{applicationId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> rejectApplication(
            @PathVariable Long applicationId,
            @RequestParam Long employerId
    ) {
        Long rejectedId = matchsService.rejectApplication(employerId, applicationId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("applicationId", rejectedId)));
    }

    @GetMapping("/api/v1/freelancer/application")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ApplicationResponseDTO>>> getFreelancerApplications(
            @RequestParam Long freelancerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<ApplicationResponseDTO> result = matchsService.getFreelancerApplications(freelancerId);
        return ResponseEntity.ok(ApiResponse.ok(toPagedResponse(result, page, size)));
    }

    @GetMapping("/api/v1/freelancer/application/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> getFreelancerApplication(
            @PathVariable Long applicationId,
            @RequestParam Long freelancerId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(matchsService.getFreelancerApplication(freelancerId, applicationId)));
    }

    @GetMapping("/api/v1/freelancer/proposal")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ProposalResponseDTO>>> getFreelancerProposals(
            @RequestParam Long freelancerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<ProposalResponseDTO> result = matchsService.getFreelancerProposals(freelancerId);
        return ResponseEntity.ok(ApiResponse.ok(toPagedResponse(result, page, size)));
    }

    @GetMapping("/api/v1/freelancer/proposal/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalResponseDTO>> getFreelancerProposal(
            @PathVariable Long proposalId,
            @RequestParam Long freelancerId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(matchsService.getFreelancerProposal(freelancerId, proposalId)));
    }

    @PatchMapping("/api/v1/freelancer/deny/{proposalId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> rejectProposal(
            @PathVariable Long proposalId,
            @RequestParam Long freelancerId
    ) {
        Long rejectedId = matchsService.rejectProposal(freelancerId, proposalId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("proposalId", rejectedId)));
    }

    @PatchMapping("/api/v1/freelancer/agree/{proposalId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> acceptProposal(
            @PathVariable Long proposalId,
            @RequestParam Long freelancerId
    ) {
        Long projectId = matchsService.acceptProposal(freelancerId, proposalId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("projectId", projectId)));
    }

    private <T> PagedResponseDTO<T> toPagedResponse(List<T> source, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        long sourceSize = source.size();
        long startLong = Math.min((long) safePage * (long) safeSize, sourceSize);
        int fromIndex = (int) startLong;
        long endLong = Math.min(startLong + (long) safeSize, sourceSize);
        int toIndex = (int) endLong;
        int totalPages = (int) Math.ceil((double) source.size() / safeSize);

        return new PagedResponseDTO<>(
                source.subList(fromIndex, toIndex),
                safePage,
                safeSize,
                source.size(),
                totalPages
        );
    }
}
