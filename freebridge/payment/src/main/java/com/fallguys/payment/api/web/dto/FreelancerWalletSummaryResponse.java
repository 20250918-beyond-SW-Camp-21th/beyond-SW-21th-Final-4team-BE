package com.fallguys.payment.api.web.dto;

public record FreelancerWalletSummaryResponse(
        Long totalEarned,
        Long pendingAmount,
        Integer transactionCount
) {}
