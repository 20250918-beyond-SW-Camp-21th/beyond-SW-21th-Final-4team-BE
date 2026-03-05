package com.fallguys.payment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.AdminSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin Settlement", description = "정산 관리자 API")
@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;

    @Operation(summary = "[Admin] 정산 레코드 수동 생성", description = "특정 계약의 정산 레코드를 수동으로 생성 (정상 시나리오에서는 계약 서명 시 자동 처리)")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Void>> generate(
            @RequestParam Long contractId) {

        adminSettlementService.generateSettlements(contractId);
        ApiResponse<Void> apiResponse = ApiResponse.ok(null);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "[Admin] 자동 지급 스케줄러 수동 실행", description = "scheduledDate <= 오늘인 PENDING 상태의 프리랜서 정산을 수동으로 지급 처리")
    @PostMapping("/disburse/run")
    public ResponseEntity<ApiResponse<Void>> runDisbursement() {

        adminSettlementService.runDisbursement();
        ApiResponse<Void> apiResponse = ApiResponse.ok(null);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    /*
     * @Operation(summary = "[Admin] 계약 정산 취소",
     * description =
     * "PAID 상태(에스크로)인 미지급 회차만 취소. DISBURSED 회차는 이미 프리랜서에게 지급 완료로 제외.")
     * 
     * @PostMapping("/cancel")
     * public ResponseEntity<ApiResponse<CancellationResult>> cancel(@RequestParam
     * Long contractId) {
     * CancellationResult result =
     * adminSettlementService.cancelContractSettlements(contractId);
     * ApiResponse<CancellationResult> apiResponse = ApiResponse.ok(result);
     * return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
     * }
     */

    @Operation(summary = "[Admin] 전체 정산 목록 조회", description = "모든 계약의 정산 레코드 조회 (상태 필터링 가능)")
    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<PageResponse<EmployerSettlementItem>>> listAll(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<EmployerSettlementItem> response = adminSettlementService.listAllSettlements(status, page, size);
        ApiResponse<PageResponse<EmployerSettlementItem>> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}
