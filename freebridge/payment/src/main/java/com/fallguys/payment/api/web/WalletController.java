package com.fallguys.payment.api.web;

import com.fallguys.common.response.ApiResponse;
import com.fallguys.common.security.CustomUserDetails;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Wallet", description = "지갑 및 거래 내역 API")
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    // ─── Employer ───────────────────────────────────────────────────────────

    @Operation(summary = "고용주 지갑 요약 조회", description = "총 지출액 및 거래 건수")
    @PreAuthorize("hasRole('EMPLOYER')")
    @GetMapping("/employer/summary")
    public ResponseEntity<ApiResponse<EmployerWalletSummaryResponse>> employerSummary(
            @AuthenticationPrincipal CustomUserDetails user) {

        EmployerWalletSummaryResponse response = walletService.getEmployerSummary(user.getId());
        ApiResponse<EmployerWalletSummaryResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "고용주 거래 내역 조회", description = "계약 결제, 구독 결제, 환불 내역 페이지네이션 조회")
    @PreAuthorize("hasRole('EMPLOYER')")
    @GetMapping("/employer/transactions")
    public ResponseEntity<ApiResponse<PageResponse<WalletTransactionItem>>> employerTransactions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "ALL") String referenceType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<WalletTransactionItem> response =
                walletService.getEmployerTransactions(user.getId(), referenceType, page, size);
        ApiResponse<PageResponse<WalletTransactionItem>> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // ─── Freelancer ──────────────────────────────────────────────────────────

    @Operation(summary = "프리랜서 지갑 요약 조회", description = "총 수령액, 지급 예정 금액 및 거래 건수")
    @PreAuthorize("hasRole('FREELANCER')")
    @GetMapping("/freelancer/summary")
    public ResponseEntity<ApiResponse<FreelancerWalletSummaryResponse>> freelancerSummary(
            @AuthenticationPrincipal CustomUserDetails user) {

        FreelancerWalletSummaryResponse response = walletService.getFreelancerSummary(user.getId());
        ApiResponse<FreelancerWalletSummaryResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "프리랜서 거래 내역 조회", description = "지급 내역 페이지네이션 조회")
    @PreAuthorize("hasRole('FREELANCER')")
    @GetMapping("/freelancer/transactions")
    public ResponseEntity<ApiResponse<PageResponse<WalletTransactionItem>>> freelancerTransactions(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<WalletTransactionItem> response =
                walletService.getFreelancerTransactions(user.getId(), page, size);
        ApiResponse<PageResponse<WalletTransactionItem>> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    // ─── Admin / Platform ────────────────────────────────────────────────────

    @Operation(summary = "[Admin] 플랫폼 에스크로 잔액 조회",
            description = "프리랜서에게 지급 예정인 보유 금액 (PLATFORM_ESCROW 지갑)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/platform/escrow")
    public ResponseEntity<ApiResponse<PlatformWalletResponse>> escrowBalance() {

        PlatformWalletResponse response = walletService.getEscrowWallet();
        ApiResponse<PlatformWalletResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }

    @Operation(summary = "[Admin] 플랫폼 수익 잔액 조회",
            description = "수수료 및 구독 수익 (PLATFORM_REVENUE 지갑)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/platform/revenue")
    public ResponseEntity<ApiResponse<PlatformWalletResponse>> revenueBalance() {

        PlatformWalletResponse response = walletService.getRevenueWallet();
        ApiResponse<PlatformWalletResponse> apiResponse = ApiResponse.ok(response);
        return ResponseEntity.status(apiResponse.httpStatus()).body(apiResponse);
    }
}