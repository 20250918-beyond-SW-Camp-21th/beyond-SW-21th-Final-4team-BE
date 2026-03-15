package com.fallguys.contract.api.web;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.contract.api.web.dto.*;
import com.fallguys.contract.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Contract", description = "계약 관련 API")
@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "계약 생성", description = "EMPLOYER 권한의 사용자가 프리랜서와의 계약서를 생성합니다. 생성 시 계약 상태는 WAITING_SIGNATURE이며, 요청 바디에 고용주 서명(employerSignature)을 포함할 수 있습니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ContractResponse>> create(
            @Valid @RequestBody CreateContractRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getId();
        String role = principal.getRole();

        // Only EMPLOYER can create contracts
        if (!"EMPLOYER".equalsIgnoreCase(role)) {
            throw new BusinessException(ErrorCode.CONTRACT_FORBIDDEN);
        }

        ContractResponse response = contractService.createContract(request, userId);
        ApiResponse<ContractResponse> apiResponse = ApiResponse.created(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "계약 목록 조회", description = "로그인한 사용자의 계약 목록을 페이지네이션으로 조회합니다. EMPLOYER/FREELANCER 역할에 따라 본인과 관련된 계약만 반환됩니다. status 파라미터로 복수 상태 필터링 가능합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ContractListResponse>> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        Long userId = principal.getId();
        String userRole = principal.getRole();

        ContractListResponse response = contractService.listContracts(
                userId, userRole, status, search, page, limit);
        ApiResponse<ContractListResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }


    @Operation(summary = "계약 단건 조회", description = "특정 계약의 상세 정보를 조회합니다. 본인이 고용주 또는 프리랜서로 등록된 계약만 조회 가능합니다.")
    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<ContractResponse>> getOne(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getId();

        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(contractService.getContract(contractId, userId));
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @PostMapping("/{contractId}/ai-review")
    public ResponseEntity<ApiResponse<ContractResponse>> requestAiReview(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Long userId = principal.getId();

        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(
                contractService.requestAiLegalReview(contractId, userId)
        );
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "계약 서명", description = "EMPLOYER 또는 FREELANCER가 계약서에 서명합니다. 양 당사자 모두 서명 완료 시 계약 상태가 WAITING_SIGNATURE → IN_PROGRESS로 전환됩니다.")
    @PatchMapping("/{contractId}/sign")
    public ResponseEntity<ApiResponse<ContractResponse>> sign(
            @PathVariable Long contractId,
            @RequestBody SignContractRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getId();
        String userRole = principal.getRole();

        ContractResponse response = contractService.sign(contractId, request, userRole, userId);
        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "계약 완료 처리", description = "EMPLOYER 권한의 사용자가 계약을 완료 처리합니다. 계약 상태가 COMPLETED로 전환됩니다.")
    @PatchMapping("/{contractId}/complete")
    public ResponseEntity<ApiResponse<ContractResponse>> complete(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getId();
        String role = principal.getRole();

        // Only EMPLOYER can complete contracts
        if (!"EMPLOYER".equalsIgnoreCase(role)) {
            throw new BusinessException(ErrorCode.ONLY_EMPLOYER_ALLOWED);
        }

        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(contractService.complete(contractId, userId));
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "계약 거절", description = "계약 당사자(EMPLOYER 또는 FREELANCER)가 계약을 거절합니다. 계약 상태가 REJECTED로 전환됩니다.")
    @PatchMapping("/{contractId}/reject")
    public ResponseEntity<ApiResponse<ContractResponse>> reject(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getId();

        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(contractService.reject(contractId, userId));
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "계약서 PDF URL 조회", description = "계약서의 PDF 파일 URL을 반환합니다. 서명 완료 전에는 미리보기 PDF URL, 서명 완료 후에는 서명본 PDF URL이 반환됩니다.")
    @GetMapping("/{contractId}/pdf")
    public ResponseEntity<ApiResponse<String>> getPdf(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails principal) {

        Long userId = principal.getId();

        String downloadUrl = contractService.getPdfDownloadUrl(contractId, userId);
        ApiResponse<String> apiResponse = ApiResponse.ok(downloadUrl);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
