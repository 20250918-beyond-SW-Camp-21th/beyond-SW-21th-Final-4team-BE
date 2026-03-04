package com.fallguys.payment.api.web.dto;

public record EmployerWalletSummaryResponse(
        Long totalPaidOut,
        Integer transactionCount
) {}
