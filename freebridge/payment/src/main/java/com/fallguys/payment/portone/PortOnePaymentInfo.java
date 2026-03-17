package com.fallguys.payment.portone;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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
     * 포트원 V2 customData는 문자열 또는 JSON 객체 형태로 반환될 수 있습니다.
     * 프론트엔드에서 JSON.stringify({contractId, employerId})로 전달하더라도
     * 포트원 SDK가 객체로 변환하여 저장할 수 있으므로 JsonNode로 받습니다.
     */
    @JsonProperty("customData")
    private JsonNode customDataNode;

    /**
     * customData를 문자열로 반환합니다 (디버그 로그용).
     */
    @JsonIgnore
    public String getCustomDataRaw() {
        if (customDataNode == null || customDataNode.isNull()) return null;
        return customDataNode.isTextual() ? customDataNode.asText() : customDataNode.toString();
    }

    /**
     * customData를 CustomDataInfo 객체로 파싱해서 반환합니다.
     * 포트원 SDK가 JSON.stringify 결과를 한 번 더 직렬화하여 삼중 인코딩될 수 있으므로
     * 문자열로 시작하는 경우 한 단계 더 언래핑합니다.
     * 파싱 실패 시 null을 반환합니다.
     */
    @JsonIgnore
    public CustomDataInfo getCustomData() {
        if (customDataNode == null || customDataNode.isNull()) return null;
        try {
            String raw;
            if (customDataNode.isTextual()) {
                raw = customDataNode.asText();
            } else {
                // JSON 객체 형태: {"contractId":1,"employerId":2}
                return OBJECT_MAPPER.treeToValue(customDataNode, CustomDataInfo.class);
            }
            if (raw == null || raw.isBlank()) return null;
            // 삼중 인코딩 처리: raw가 JSON string("\"{...}\"")으로 감싸진 경우 한 번 더 언래핑
            if (raw.startsWith("\"")) {
                raw = OBJECT_MAPPER.readValue(raw, String.class);
            }
            return OBJECT_MAPPER.readValue(raw, CustomDataInfo.class);
        } catch (Exception e) {
            log.warn("customData 파싱 실패 (원문: {}): {}", customDataNode, e.getMessage());
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
        private String mode;
        private String planType;
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
