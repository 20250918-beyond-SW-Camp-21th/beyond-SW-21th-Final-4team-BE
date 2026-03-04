package com.fallguys.payment.api.web.dto;

import java.time.LocalDate;

public record FreelancerSettlementItem(
        Long id,
        Long contractId,
        Long employerSettlementId,
        String projectName,
        String employerName,
        Long totalAmount,
        Long platformFee,
        Long tax,
        Long netAmount,
        Integer installmentNumber,
        String status,
        LocalDate scheduledDate,
        LocalDate paidDate,
        String receiptPdfUrl
) {}
