package com.fallguys.payment.api.web.dto;

import java.time.LocalDateTime;

public record WalletTransactionItem(
        Long id,
        String type,
        Long amount,
        String referenceType,
        Long referenceId,
        String description,
        Long balanceAfter,
        LocalDateTime createdAt
) {}
