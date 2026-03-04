package com.fallguys.payment.api.web.dto;

public record WebhookPayload(
        String type,
        String timestamp,
        WebhookData data
) {
    // 내부 데이터 용 Record
    public record WebhookData(
            String paymentId,
            String storeId,
            String transactionId
    ) {}
}