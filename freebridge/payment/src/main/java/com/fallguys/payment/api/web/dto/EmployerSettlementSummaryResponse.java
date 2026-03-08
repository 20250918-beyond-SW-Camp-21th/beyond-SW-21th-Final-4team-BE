package com.fallguys.payment.api.web.dto;

public record EmployerSettlementSummaryResponse(
        Long totalPaidAmount,
        Long totalDisbursedAmount,
        Integer paidCount,
        Integer disbursedCount,
        Integer cancelledCount
) {}
