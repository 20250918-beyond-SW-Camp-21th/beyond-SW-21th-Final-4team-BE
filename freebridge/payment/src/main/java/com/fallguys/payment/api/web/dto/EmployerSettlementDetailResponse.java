package com.fallguys.payment.api.web.dto;

import java.time.LocalDate;

public record EmployerSettlementDetailResponse(
        Long id,
        Long contractId,
        String projectName,
        String freelancerName,
        Long billingAmount,
        Long platformFee,
        Double commissionRate,
        Long totalPayment,
        Integer installmentNumber,
        String status,
        String invoicePdfUrl,
        LocalDate dueDate,
        LocalDate paidDate
) {}
