package com.fallguys.payment.api.web.dto;

import java.time.LocalDate;

public record SubscriptionBillingItem(
        Long id,
        String planType,
        Long amount,
        String status,
        LocalDate billingDate,
        LocalDate paidDate,
        String invoicePdfUrl
) {}
