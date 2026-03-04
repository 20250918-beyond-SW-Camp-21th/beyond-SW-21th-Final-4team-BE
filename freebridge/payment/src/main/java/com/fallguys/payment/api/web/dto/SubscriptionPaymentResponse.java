package com.fallguys.payment.api.web.dto;

import java.time.LocalDate;

public record SubscriptionPaymentResponse(
        boolean success,
        Long billingId,
        Long employerId,
        String planType,
        Long amount,
        String status,
        LocalDate paidDate,
        // Populated only on failure
        String errorCode,
        String message
) {}
