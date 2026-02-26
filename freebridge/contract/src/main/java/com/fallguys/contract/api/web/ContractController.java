package com.fallguys.contract.api.web;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.response.ApiResponse;
import com.fallguys.contract.api.web.dto.*;
import com.fallguys.contract.service.ContractService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Contract", description = "계약 관련 API")
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    //계약 생성
    @PostMapping
    public ResponseEntity<ApiResponse<ContractResponse>> create(
            @Valid @RequestBody CreateContractRequest request,
            @AuthenticationPrincipal Map<String, Object> principal) {

        Long userId = ((Number) principal.get("id")).longValue();
        String role = (String) principal.get("role");

        // Only EMPLOYER can create contracts
        if (!"EMPLOYER".equalsIgnoreCase(role)) {
            throw new BusinessException(ErrorCode.ONLY_EMPLOYER_ALLOWED);
        }

        ContractResponse response = contractService.createContract(request, userId);
        ApiResponse<ContractResponse> apiResponse = ApiResponse.created(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // 계약 미리보기
    @GetMapping
    public ResponseEntity<ApiResponse<ContractListResponse>> list(
            @AuthenticationPrincipal Map<String, Object> principal,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        Long userId = ((Number) principal.get("id")).longValue();
        String userRole = (String) principal.get("role");

        ContractListResponse response = contractService.listContracts(
                userId, userRole, status, search, page, limit);
        ApiResponse<ContractListResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }


    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<ContractResponse>> getOne(
            @PathVariable Long contractId,
            @AuthenticationPrincipal Map<String, Object> principal) {

        Long userId = ((Number) principal.get("id")).longValue();

        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(contractService.getContract(contractId, userId));
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // 서명 후 계약 상태 변환
    @PatchMapping("/{contractId}/sign")
    public ResponseEntity<ApiResponse<ContractResponse>> sign(
            @PathVariable Long contractId,
            @RequestBody SignContractRequest request,
            @AuthenticationPrincipal Map<String, Object> principal) {

        Long userId = ((Number) principal.get("id")).longValue();
        String userRole = (String) principal.get("role");

        ContractResponse response = contractService.sign(contractId, request.getSignature(), userRole, userId);
        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @PatchMapping("/{contractId}/complete")
    public ResponseEntity<ApiResponse<ContractResponse>> complete(
            @PathVariable Long contractId,
            @AuthenticationPrincipal Map<String, Object> principal) {

        Long userId = ((Number) principal.get("id")).longValue();
        String role = (String) principal.get("role");

        // Only EMPLOYER can complete contracts
        if (!"EMPLOYER".equalsIgnoreCase(role)) {
            throw new BusinessException(ErrorCode.ONLY_EMPLOYER_ALLOWED);
        }

        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(contractService.complete(contractId, userId));
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @PatchMapping("/{contractId}/reject")
    public ResponseEntity<ApiResponse<ContractResponse>> reject(
            @PathVariable Long contractId,
            @AuthenticationPrincipal Map<String, Object> principal) {

        Long userId = ((Number) principal.get("id")).longValue();

        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(contractService.reject(contractId, userId));
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: AWS에 올리면 E3에서 제대로 된 주소로 반환하게 수정
    @GetMapping("/{contractId}/pdf")
    public ResponseEntity<ApiResponse<String>> getPdf(
            @PathVariable Long contractId,
            @AuthenticationPrincipal Map<String, Object> principal) {

        Long userId = ((Number) principal.get("id")).longValue();

        String pdfUrl = contractService.getContract(contractId, userId).getContractPdfUrl();
        ApiResponse<String> apiResponse = ApiResponse.ok(pdfUrl);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}