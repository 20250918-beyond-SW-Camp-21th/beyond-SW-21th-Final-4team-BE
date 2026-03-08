package com.fallguys.payment.api.web.dto;

public record FreelancerSettlementSummaryResponse(
        Long pendingAmount,
        Integer pendingCount,
        Long paidAmount,
        Integer paidCount
) {}
