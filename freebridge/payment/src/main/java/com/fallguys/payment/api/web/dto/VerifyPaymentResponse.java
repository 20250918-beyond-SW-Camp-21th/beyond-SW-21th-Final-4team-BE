package com.fallguys.payment.api.web.dto;

public record VerifyPaymentResponse(
        boolean success,
        Long contractId,
        Long totalVerifiedAmount,
        Integer installmentsCreated
) {}
