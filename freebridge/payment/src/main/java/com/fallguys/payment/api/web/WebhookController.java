package com.fallguys.payment.api.web;

import com.fallguys.payment.api.web.dto.WebhookPayload;
import com.fallguys.payment.portone.PortOneApiClient;
import com.fallguys.payment.portone.PortOnePaymentInfo;
import com.fallguys.payment.service.EmployerSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import com.fallguys.common.exception.BusinessException;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class WebhookController {

    private final EmployerSettlementService settlementService;
    private final PortOneApiClient portOneApiClient;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody WebhookPayload payload) {
        log.info("포트원 웹훅 수신: {}", payload);

        try {
            // Record 방식: payload.getType() -> payload.type()
            if ("Transaction.Paid".equals(payload.type())) {

                if (payload.data() == null) {
                    log.warn("웹훅 데이터 누락: payload={}", payload);
                    return ResponseEntity.ok("Invalid payload");
                }
                // Record 방식: payload.getData().getPaymentId() -> payload.data().paymentId()
                String paymentId = payload.data().paymentId();

                // 2. 포트원 API로 결제 내역 교차 검증 (보안)
                PortOnePaymentInfo paymentInfo = portOneApiClient.getPayment(paymentId);

                // 3. 실제 PAID 상태가 맞는지 확인
                if (paymentInfo.isPaid() && paymentInfo.getCustomData() != null) {
                    Long contractId = paymentInfo.getCustomData().getContractId();
                    Long employerId = paymentInfo.getCustomData().getEmployerId();

                    if (contractId == null || employerId == null) {
                        log.warn("웹훅 customData에 contractId 또는 employerId 누락: paymentId={}", paymentId);
                        return ResponseEntity.ok("Missing metadata");
                    }

                    // 4. 계약 대금 선결제 및 에스크로 보관 로직 실행 (멱등성 보장 필수)
                    settlementService.verifyContractPayment(paymentId, contractId, employerId);
                }

            } else if ("Transaction.Failed".equals(payload.type())) {

                if (payload.data() == null) {
                    log.warn("웹훅 데이터 누락 (Failed): payload={}", payload);
                    return ResponseEntity.ok("Invalid payload");
                }
                String paymentId = payload.data().paymentId();
                log.warn("포트원 결제 실패 웹훅 수신: paymentId={}", paymentId);
                // 정상 흐름에서는 PAID 확인 후 정산 레코드가 생성되므로
                // 결제 실패 시 생성된 레코드가 없어 추가 처리 불필요
            }

            return ResponseEntity.ok("OK");

        } catch (BusinessException e) {
            log.error("웹훅 비즈니스 처리 실패", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Rejected");
        } catch (Exception e) {
            log.error("웹훅 처리 중 시스템 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Retry later");
        }
    }
}