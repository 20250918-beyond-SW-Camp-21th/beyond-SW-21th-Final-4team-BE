package com.fallguys.payment.api.web.dto;

import java.time.LocalDate;

public record FreelancerSettlementDetailResponse(
        Long id,
        Long contractId,
        Long employerSettlementId,
        String projectName,
        String employerName,
        Integer paymentDay,
        Long totalAmount,
        Long platformFee,
        Double commissionRate,
        Long tax,
        Long netAmount,
        Integer installmentNumber,
        String status,
        LocalDate scheduledDate,
        LocalDate paidDate,
        String receiptPdfUrl
) {}
