package com.fallguys.matchs.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.matchs.api.dto.request.ApplicationCreateRequest;
import com.fallguys.matchs.api.dto.request.ProposalCreateRequest;
import com.fallguys.matchs.service.MatchsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MatchsController {

    private final MatchsService matchsService;

    @PostMapping("/api/v1/matches/applications")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createApplication(
            @RequestParam Long freelancerId,
            @RequestBody ApplicationCreateRequest request
    ) {
        Long applicationId = matchsService.createApplication(freelancerId, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("applicationId", applicationId)));
    }

    @PostMapping("/api/v1/matches/proposals")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createProposal(
            @RequestParam Long employerId,
            @RequestBody ProposalCreateRequest request
    ) {
        Long proposalId = matchsService.createProposal(employerId, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("proposalId", proposalId)));
    }

    @PostMapping("/api/v1/matches/applications/{applicationId}/accept")
    public ResponseEntity<ApiResponse<Map<String, Long>>> acceptApplication(
            @PathVariable Long applicationId,
            @RequestParam Long employerId
    ) {
        Long projectId = matchsService.acceptApplication(employerId, applicationId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("projectId", projectId)));
    }

    @PostMapping("/api/v1/matches/proposals/{proposalId}/accept")
    public ResponseEntity<ApiResponse<Map<String, Long>>> acceptProposal(
            @PathVariable Long proposalId,
            @RequestParam Long freelancerId
    ) {
        Long projectId = matchsService.acceptProposal(freelancerId, proposalId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("projectId", projectId)));
    }
}
