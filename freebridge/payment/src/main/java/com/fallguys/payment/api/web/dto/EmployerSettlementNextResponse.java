package com.fallguys.payment.api.web.dto;

import java.time.LocalDate;

public record EmployerSettlementNextResponse(
        Long id,
        Long contractId,
        String projectName,
        String freelancerName,
        Long billingAmount,
        Long platformFee,
        Long totalPayment,
        Integer installmentNumber,
        LocalDate scheduledDisbursementDate,
        String status
) {}
