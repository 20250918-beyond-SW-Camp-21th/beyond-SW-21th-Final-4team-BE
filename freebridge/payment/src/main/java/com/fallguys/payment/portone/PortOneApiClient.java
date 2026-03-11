package com.fallguys.payment.portone;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.payment.config.PortOneProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

/**
 * PortOne V2 REST API 클라이언트
 * WebClient를 사용해 포트원 API를 직접 호출합니다.
 *
 * 테스트 모드: PORTONE_API_SECRET 환경변수에 v2_test_... 키를 설정하면
 * 테스트 카드(예: 4111 1111 1111 1111)로 실제 돈 없이 결제 흐름을 체험할 수 있습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortOneApiClient {

    @Qualifier("portOneWebClient")
    private final WebClient webClient;

    private final PortOneProperties portOneProperties;

    private final ObjectMapper objectMapper;

    /**
     * 결제 정보 단건 조회
     * GET /payments/{paymentId}
     */
    public PortOnePaymentInfo getPayment(String paymentId) {
        try {
            // 먼저 raw String으로 받아 응답 본문을 로그에 찍은 뒤 수동 파싱
            String rawBody = webClient.get()
                    .uri("/payments/{paymentId}", paymentId)
                    .retrieve()
                    .onStatus(HttpStatus.NOT_FOUND::equals,
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)))
                    .bodyToMono(String.class)
                    .block();

            if (rawBody == null || rawBody.isBlank()) {
                log.error("PortOne getPayment 빈 응답: paymentId={}", paymentId);
                throw new BusinessException(ErrorCode.PAYMENT_FAILED);
            }

            return objectMapper.readValue(rawBody, PortOnePaymentInfo.class);

        } catch (BusinessException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("PortOne getPayment HTTP 오류: paymentId={}, status={}, body={}",
                    paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND);
            }
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        } catch (Exception e) {
            log.error("PortOne getPayment 처리 오류: paymentId={}, exceptionType={}, message={}",
                    paymentId, e.getClass().getName(), e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }

    /**
     * 빌링키로 즉시 결제
     * POST /payments/{paymentId}/billing-key
     *
     * [포트원 V2 응답 구조 주의]
     * 빌링키 결제 API의 응답은 {"payment": {"pgTxId": "...", "paidAt": "..."}} 형태로
     * pgTxId와 paidAt만 포함합니다. status나 amount 등 전체 결제 정보가 없으므로
     * 결제 요청 후 getPayment()를 별도 호출하여 실제 결제 상태를 확인합니다.
     *
     * 테스트 모드에서는 테스트 빌링키를 사용하며 실제 결제가 발생하지 않습니다.
     * channelKey를 명시하여 테스트 채널로 정확히 라우팅합니다.
     */
    public PortOnePaymentInfo chargeBillingKey(String paymentId, String billingKey, long amount, String orderName,
            String customerId) {

        Map<String, Object> body = new HashMap<>();
        body.put("billingKey", billingKey);
        body.put("orderName", orderName);
        body.put("amount", Map.of("total", amount));
        body.put("currency", "KRW");
        body.put("customer", Map.of("id", customerId));

        // 테스트/라이브 채널 명시 — application.yml의 portone.channel-key 사용
        String channelKey = portOneProperties.getChannelKey();
        if (channelKey != null && !channelKey.isBlank()) {
            body.put("channelKey", channelKey);
        }

        log.info("PortOne 빌링키 결제 요청: paymentId={}, amount={}, customerId={}", paymentId, amount, customerId);

        try {
            // 빌링키 결제 요청 — 응답 본문(pgTxId, paidAt)은 사용하지 않고 소비만 함
            webClient.post()
                    .uri("/payments/{paymentId}/billing-key", paymentId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("PortOne chargeBillingKey 오류: billingKey={}, status={}, body={}",
                    billingKey, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        // 실제 결제 상태(PAID 여부, amount 등)를 getPayment()로 재조회하여 반환
        log.info("PortOne 빌링키 결제 완료, 결제 상태 재조회: paymentId={}", paymentId);
        return getPayment(paymentId);
    }

    /**
     * 포트원 결제 부분 취소 (보상 트랜잭션용)
     * POST /payments/{paymentId}/cancel
     */
    public PortOnePaymentInfo cancelPayment(String paymentId, long amount, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount);
        body.put("reason", reason);

        log.info("PortOne 결제 취소 요청: paymentId={}, amount={}, reason={}", paymentId, amount, reason);

        try {
            String rawBody = webClient.post()
                    .uri("/payments/{paymentId}/cancel", paymentId)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (rawBody == null || rawBody.isBlank()) {
                log.warn("PortOne cancelPayment 빈 응답: paymentId={}", paymentId);
                return null;
            }

            return objectMapper.readValue(rawBody, PortOnePaymentInfo.class);

        } catch (WebClientResponseException e) {
            log.error("PortOne cancelPayment 오류: paymentId={}, status={}, body={}",
                    paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        } catch (Exception e) {
            log.error("PortOne cancelPayment 처리 오류: paymentId={}, exceptionType={}, message={}",
                    paymentId, e.getClass().getName(), e.getMessage());
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }
    }
}
