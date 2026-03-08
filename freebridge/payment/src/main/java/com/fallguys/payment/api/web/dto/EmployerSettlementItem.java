package com.fallguys.payment.api.web.dto;

import java.time.LocalDate;

public record EmployerSettlementItem(
        Long id,
        Long contractId,
        String projectName,
        String freelancerName,
        Long billingAmount,
        Long platformFee,
        Long totalPayment,
        Integer installmentNumber,
        String status,
        String invoicePdfUrl,
        LocalDate dueDate,
        LocalDate paidDate
) {}
