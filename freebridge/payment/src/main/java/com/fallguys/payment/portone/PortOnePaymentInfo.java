package com.fallguys.payment.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PortOne V2 결제 정보 응답 DTO
 * GET /payments/{paymentId} 응답 매핑
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOnePaymentInfo {

    @JsonProperty("id")
    private String paymentId;

    private String status;

    private AmountInfo amount;

    private String currency;

    private String orderName;

    @JsonProperty("method")
    private MethodInfo method;

    private CustomDataInfo customData;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomDataInfo {
        private Long contractId;
        private Long employerId;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AmountInfo {
        private Long total;
        private Long taxFree;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MethodInfo {
        private Object card;
        private Object virtualAccount;
        private Object easyPay;
    }

    public boolean isPaid() {
        return "PAID".equals(status);
    }

    public long getTotalAmount() {
        return amount != null && amount.getTotal() != null ? amount.getTotal() : 0L;
    }
}
