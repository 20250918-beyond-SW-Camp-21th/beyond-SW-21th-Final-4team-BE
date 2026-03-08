package com.fallguys.payment.api.web.dto;

import java.time.LocalDateTime;

public record TaxInvoiceResponse(
        Long taxInvoiceId,
        Long settlementId,
        String status,
        LocalDateTime requestedAt
) {}
