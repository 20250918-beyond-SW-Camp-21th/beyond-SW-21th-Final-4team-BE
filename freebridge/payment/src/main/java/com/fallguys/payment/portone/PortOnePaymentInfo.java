package com.fallguys.payment.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * PortOne V2 결제 정보 응답 DTO
 * GET /payments/{paymentId} 응답 매핑
 *
 * [주의] 포트원 V2 스펙상 customData는 string 타입(JSON 직렬화 문자열)으로 반환됩니다.
 * 따라서 customDataRaw로 원문 문자열을 받은 뒤 getCustomData()로 파싱합니다.
 */
@Slf4j
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortOnePaymentInfo {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @JsonProperty("id")
    private String paymentId;

    private String status;

    private AmountInfo amount;

    private String currency;

    private String orderName;

    @JsonProperty("method")
    private MethodInfo method;

    /**
     * 포트원 V2 customData는 string 타입입니다.
     * 프론트엔드에서 JSON.stringify({contractId, employerId}) 형태로 전달하면
     * 포트원이 문자열 그대로 저장하고 반환합니다.
     */
    @JsonProperty("customData")
    private String customDataRaw;

    /**
     * customDataRaw(JSON 문자열)를 CustomDataInfo 객체로 파싱해서 반환합니다.
     * 파싱 실패 시 null을 반환합니다.
     */
    public CustomDataInfo getCustomData() {
        if (customDataRaw == null || customDataRaw.isBlank()) return null;
        try {
            return OBJECT_MAPPER.readValue(customDataRaw, CustomDataInfo.class);
        } catch (Exception e) {
            log.warn("customData 파싱 실패 (원문: {}): {}", customDataRaw, e.getMessage());
            return null;
        }
    }

    @Getter
    @Setter
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
