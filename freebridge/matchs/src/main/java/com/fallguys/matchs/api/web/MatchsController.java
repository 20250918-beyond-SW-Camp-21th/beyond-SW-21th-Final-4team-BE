package com.fallguys.matchs.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.matchs.api.dto.request.ApplicationCreateRequest;
import com.fallguys.matchs.api.dto.request.ProposalCreateRequest;
import com.fallguys.matchs.api.dto.response.ApplicationResponseDTO;
import com.fallguys.matchs.api.dto.response.PagedResponseDTO;
import com.fallguys.matchs.api.dto.response.ProposalResponseDTO;
import com.fallguys.matchs.api.support.TokenUserIdResolver;
import com.fallguys.matchs.service.MatchsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Matchs", description = "지원 및 제안 매칭 관리 API")
public class MatchsController {

    private static final int MAX_PAGE_SIZE = 100;

    private final MatchsService matchsService;
    private final TokenUserIdResolver tokenUserIdResolver;

    @Operation(summary = "프로젝트 지원 등록", description = "프리랜서가 프로젝트에 지원합니다.")
    @PostMapping("/api/freelancer/application")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createApplication(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ApplicationCreateRequest request
    ) {
        Long freelancerId = tokenUserIdResolver.resolveUserId(authorization);
        Long applicationId = matchsService.createApplication(freelancerId, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("applicationId", applicationId)));
    }

    @Operation(summary = "프리랜서 제안 등록", description = "고용주가 프리랜서에게 제안을 보냅니다.")
    @PostMapping("/api/employer/proposals")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createProposal(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ProposalCreateRequest request
    ) {
        Long employerId = tokenUserIdResolver.resolveUserId(authorization);
        Long proposalId = matchsService.createProposal(employerId, request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("proposalId", proposalId)));
    }

    @Operation(summary = "제안 상세 조회(고용주)", description = "고용주가 발송한 제안의 상세 정보를 조회합니다.")
    @GetMapping("/api/employer/proposals/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalResponseDTO>> getEmployerProposal(
            @PathVariable Long proposalId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long employerId = tokenUserIdResolver.resolveUserId(authorization);
        return ResponseEntity.ok(ApiResponse.ok(matchsService.getEmployerProposal(employerId, proposalId)));
    }

    @Operation(summary = "제안 목록 조회(고용주)", description = "고용주가 발송한 제안 목록을 조회합니다.")
    @GetMapping("/api/employer/proposals")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ProposalResponseDTO>>> getEmployerProposals(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long employerId = tokenUserIdResolver.resolveUserId(authorization);
        Page<ProposalResponseDTO> result = matchsService.getEmployerProposals(employerId, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponseDTO<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        )));
    }

    @Operation(summary = "지원 목록 조회(고용주)", description = "고용주가 받은 지원 목록을 조회합니다.")
    @GetMapping("/api/employer/applications")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ApplicationResponseDTO>>> getEmployerApplications(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long employerId = tokenUserIdResolver.resolveUserId(authorization);
        Page<ApplicationResponseDTO> result = matchsService.getEmployerApplications(employerId, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponseDTO<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        )));
    }

    @Operation(summary = "지원 상세 조회(고용주)", description = "고용주가 특정 지원 상세 정보를 조회합니다.")
    @GetMapping("/api/employer/applications/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> getEmployerApplication(
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long employerId = tokenUserIdResolver.resolveUserId(authorization);
        return ResponseEntity.ok(ApiResponse.ok(matchsService.getEmployerApplication(employerId, applicationId)));
    }

    @Operation(summary = "지원 수락(고용주)", description = "고용주가 지원을 수락하고 프로젝트를 생성합니다.")
    @PatchMapping("/api/employer/agree/{applicationId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> acceptApplication(
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long employerId = tokenUserIdResolver.resolveUserId(authorization);
        Long projectId = matchsService.acceptApplication(employerId, applicationId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("projectId", projectId)));
    }

    @Operation(summary = "지원 거절(고용주)", description = "고용주가 지원을 거절합니다.")
    @PatchMapping("/api/employer/deny/{applicationId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> rejectApplication(
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long employerId = tokenUserIdResolver.resolveUserId(authorization);
        Long rejectedId = matchsService.rejectApplication(employerId, applicationId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("applicationId", rejectedId)));
    }

    @Operation(summary = "내 지원 목록 조회(프리랜서)", description = "프리랜서가 본인이 등록한 지원 목록을 조회합니다.")
    @GetMapping("/api/freelancer/application")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ApplicationResponseDTO>>> getFreelancerApplications(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long freelancerId = tokenUserIdResolver.resolveUserId(authorization);
        Page<ApplicationResponseDTO> result = matchsService.getFreelancerApplications(freelancerId, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponseDTO<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        )));
    }

    @Operation(summary = "내 지원 상세 조회(프리랜서)", description = "프리랜서가 본인 지원의 상세 정보를 조회합니다.")
    @GetMapping("/api/freelancer/application/{applicationId}")
    public ResponseEntity<ApiResponse<ApplicationResponseDTO>> getFreelancerApplication(
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long freelancerId = tokenUserIdResolver.resolveUserId(authorization);
        return ResponseEntity.ok(ApiResponse.ok(matchsService.getFreelancerApplication(freelancerId, applicationId)));
    }

    @Operation(summary = "받은 제안 목록 조회(프리랜서)", description = "프리랜서가 받은 제안 목록을 조회합니다.")
    @GetMapping("/api/freelancer/proposal")
    public ResponseEntity<ApiResponse<PagedResponseDTO<ProposalResponseDTO>>> getFreelancerProposals(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Long freelancerId = tokenUserIdResolver.resolveUserId(authorization);
        Page<ProposalResponseDTO> result = matchsService.getFreelancerProposals(freelancerId, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(new PagedResponseDTO<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        )));
    }

    @Operation(summary = "받은 제안 상세 조회(프리랜서)", description = "프리랜서가 받은 제안의 상세 정보를 조회합니다.")
    @GetMapping("/api/freelancer/proposal/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalResponseDTO>> getFreelancerProposal(
            @PathVariable Long proposalId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long freelancerId = tokenUserIdResolver.resolveUserId(authorization);
        return ResponseEntity.ok(ApiResponse.ok(matchsService.getFreelancerProposal(freelancerId, proposalId)));
    }

    @Operation(summary = "제안 거절(프리랜서)", description = "프리랜서가 받은 제안을 거절합니다.")
    @PatchMapping("/api/freelancer/deny/{proposalId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> rejectProposal(
            @PathVariable Long proposalId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long freelancerId = tokenUserIdResolver.resolveUserId(authorization);
        Long rejectedId = matchsService.rejectProposal(freelancerId, proposalId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("proposalId", rejectedId)));
    }

    @Operation(summary = "제안 수락(프리랜서)", description = "프리랜서가 제안을 수락하고 프로젝트를 생성합니다.")
    @PatchMapping("/api/freelancer/agree/{proposalId}")
    public ResponseEntity<ApiResponse<Map<String, Long>>> acceptProposal(
            @PathVariable Long proposalId,
            @RequestHeader("Authorization") String authorization
    ) {
        Long freelancerId = tokenUserIdResolver.resolveUserId(authorization);
        Long projectId = matchsService.acceptProposal(freelancerId, proposalId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("projectId", projectId)));
    }

}
