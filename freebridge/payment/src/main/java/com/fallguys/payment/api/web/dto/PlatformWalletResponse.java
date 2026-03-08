package com.fallguys.payment.api.web.dto;

import java.time.LocalDateTime;

public record PlatformWalletResponse(
        String walletType,
        Long balance,
        LocalDateTime updatedAt
) {}
