package com.fallguys.contract.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.contract.api.web.dto.*;
import com.fallguys.contract.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Contract", description = "계약 관련 API")
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    //계약 생성
    // TODO: 로그인 기능 구현 후 mockdata 사용에서 @Authenticated로 바꾸기
    @PostMapping
    public ResponseEntity<ApiResponse<ContractResponse>> create(
            @Valid @RequestBody CreateContractRequest request,
            @RequestHeader("X-User-Id") Long userId) {

        ContractResponse response = contractService.createContract(request, userId);
        ApiResponse<ContractResponse> apiResponse = ApiResponse.created(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // 계약 미리보기
    // TODO: 로그인 기능 구현 후 mockdata 사용에서 @Authenticated로 바꾸기
    @GetMapping
    public ResponseEntity<ApiResponse<ContractListResponse>> list(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        ContractListResponse response = contractService.listContracts(
                userId, userRole, status, search, page, limit);
        ApiResponse<ContractListResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }


    // TODO: 로그인 기능 구현 후 mockdata 사용에서 @Authenticated로 바꾸기
    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<ContractResponse>> getOne(
            @PathVariable Long contractId,
            @RequestHeader("X-User-Id") Long userId) {
        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(contractService.getContract(contractId, userId));
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // 서명 후 계약 상태 변환
    // TODO: 로그인 기능 구현 후 mockdata 사용에서 @Authenticated로 바꾸기
    @PatchMapping("/{contractId}/sign")
    public ResponseEntity<ApiResponse<ContractResponse>> sign(
            @PathVariable Long contractId,
            @RequestBody SignContractRequest request,
            @RequestHeader("X-User-Role") String userRole,
            @RequestHeader("X-User-Id") Long userId) {

        ContractResponse response = contractService.sign(contractId, request.getSignature(), userRole, userId);
        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 mockdata 사용에서 @Authenticated로 바꾸기
    @PatchMapping("/{contractId}/complete")
    public ResponseEntity<ApiResponse<ContractResponse>> complete(
            @PathVariable Long contractId,
            @RequestHeader("X-User-Id") Long userId) {
        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(contractService.complete(contractId, userId));
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: 로그인 기능 구현 후 mockdata 사용에서 @Authenticated로 바꾸기
    @PatchMapping("/{contractId}/reject")
    public ResponseEntity<ApiResponse<ContractResponse>> reject(
            @PathVariable Long contractId,
            @RequestHeader("X-User-Id") Long userId) {
        ApiResponse<ContractResponse> apiResponse = ApiResponse.ok(contractService.reject(contractId, userId));
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // TODO: AWS에 올리면 E3에서 제대로 된 주소로 반환하게 수정
    // TODO: 로그인 기능 구현 후 mockdata 사용에서 @Authenticated로 바꾸기
    @GetMapping("/{contractId}/pdf")
    public ResponseEntity<ApiResponse<String>> getPdf(
            @PathVariable Long contractId,
            @RequestHeader("X-User-Id") Long userId) {
        String pdfUrl = contractService.getContract(contractId, userId).getContractPdfUrl();
        ApiResponse<String> apiResponse = ApiResponse.ok(pdfUrl);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}