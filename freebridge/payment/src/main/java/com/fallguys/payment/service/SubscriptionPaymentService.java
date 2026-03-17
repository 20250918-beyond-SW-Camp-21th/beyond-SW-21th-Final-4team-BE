package com.fallguys.payment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.api.payment.SubscriptionPaymentQuery;
import com.fallguys.common.api.payment.SubscriptionPaymentResult;
import com.fallguys.common.api.payment.SubscriptionUpgradeResult;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.entity.*;
import com.fallguys.payment.portone.PortOneApiClient;
import com.fallguys.payment.portone.PortOnePaymentInfo;
import com.fallguys.payment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentService implements SubscriptionPaymentQuery {

    private final SubscriptionBillingRepository subscriptionBillingRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PortOneApiClient portOneApiClient;
    private final TransactionTemplate transactionTemplate;
    private final PaymentInvoicePdfService paymentInvoicePdfService;

    /**
     * 빌링키로 구독 결제 처리
     * 구독 모듈에서 billingKey를 받아 포트원으로 즉시 결제 후 SubscriptionBilling 레코드 생성
     */
    public SubscriptionPaymentResponse processPayment(SubscriptionPaymentRequest request) {
        Long employerId = request.getEmployerId();
        PlanType planType;
        try {
            // getPlanType()이 null이면 NullPointerException → IllegalArgumentException으로 잡히지 않으므로 사전 검증
            if (request.getPlanType() == null || request.getPlanType().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
            }
            planType = PlanType.valueOf(request.getPlanType().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // FREE 플랜은 결제 대상이 아님 (0원 결제 및 포트원 호출 방지)
        if (planType == PlanType.FREE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String billingKey = request.getBillingKey();
        if (employerId == null || billingKey == null || billingKey.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // 클라이언트 제공 금액 대신 서버 사이드 플랜 가격 사용 (금액 위변조 방지)
        long amount = planType.getMonthlyPrice();

        // 이중결제 방지: 5분 이내 동일 employer + planType 에 대한 SUCCESS 시도가 있으면 재결제 차단
        final PlanType finalPlanType = planType;
        Optional<PaymentAttempt> recentSuccess = paymentAttemptRepository
                .findFirstByEmployerIdAndPlanTypeAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        employerId, planType.name(), "SUCCESS", LocalDateTime.now().minusMinutes(5));
        if (recentSuccess.isPresent()) {
            log.warn("이중결제 차단: employerId={}, planType={}, recentAttemptId={}",
                    employerId, finalPlanType, recentSuccess.get().getId());
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 1. PENDING PaymentAttempt 저장 — idempotencyKey 중복 삽입 시 DataIntegrityViolationException으로
        //    TOCTOU 이중결제 원자적 차단 (soft 5분 체크는 정상 흐름에서의 조기 차단)
        String paymentId = "sub-" + UUID.randomUUID();
        final String idempotencyKey = "pay:" + employerId + ":" + planType.name() + ":" + YearMonth.now();
        try {
            transactionTemplate.executeWithoutResult(status -> {
                PaymentAttempt attempt = new PaymentAttempt();
                attempt.setId(paymentId);
                attempt.setEmployerId(employerId);
                attempt.setPlanType(planType.name());
                attempt.setStatus("PENDING");
                attempt.setIdempotencyKey(idempotencyKey);
                paymentAttemptRepository.save(attempt);
            });
        } catch (DataIntegrityViolationException e) {
            log.warn("이중결제 원자적 차단 (idempotencyKey 중복): employerId={}, planType={}, key={}",
                    employerId, planType, idempotencyKey);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        PortOnePaymentInfo paymentInfo;
        try {
            paymentInfo = portOneApiClient.chargeBillingKey(
                    paymentId, billingKey, amount,
                    planType.name() + " 구독 결제",
                    "employer-" + employerId);
        } catch (Exception e) {
            // BusinessException 및 기타 모든 예외(네트워크 오류, RuntimeException 등) 동일하게 처리
            transactionTemplate.executeWithoutResult(status -> {
                paymentAttemptRepository.findById(paymentId).ifPresent(attempt -> {
                    attempt.setStatus("FAILED");
                    attempt.setIdempotencyKey(null); // 실패 시 키 해제 — 같은 결제 기간 내 재시도 허용
                    paymentAttemptRepository.save(attempt);
                });
            });

            // 결제 실패 시 FAILED 레코드 저장 (여기도 트랜잭션으로 처리)
            return transactionTemplate.execute(status -> {
                SubscriptionBilling failedBilling = new SubscriptionBilling();
                failedBilling.setEmployerId(employerId);
                failedBilling.setPlanType(planType);
                failedBilling.setAmount(amount);
                failedBilling.setStatus(SubscriptionBillingStatus.FAILED);
                failedBilling.setBillingDate(LocalDate.now());
                subscriptionBillingRepository.save(failedBilling);

                return new SubscriptionPaymentResponse(
                        false, failedBilling.getId(), employerId, planType.name(),
                        amount, SubscriptionBillingStatus.FAILED.name(), null,
                        "PAYMENT_FAILED", e.getMessage());
            });
        }

        // 2. 외부 결제 완료 — PaymentAttempt 상태를 즉시 별도 트랜잭션에 저장하여
        //    이후 후처리 실패 시에도 결제 증거가 유실되지 않도록 함
        final boolean paid = paymentInfo.isPaid();
        final String portonePaymentId = paymentInfo.getPaymentId();

        transactionTemplate.executeWithoutResult(txStatus -> {
            paymentAttemptRepository.findById(paymentId).ifPresent(attempt -> {
                attempt.setStatus(paid ? "SUCCESS" : "FAILED");
                if (!paid) attempt.setIdempotencyKey(null); // 실패 시 키 해제 — 재시도 허용
                paymentAttemptRepository.save(attempt);
            });
        });

        if (!paid) {
            return transactionTemplate.execute(txStatus -> {
                SubscriptionBilling failedBilling = new SubscriptionBilling();
                failedBilling.setEmployerId(employerId);
                failedBilling.setPlanType(planType);
                failedBilling.setAmount(amount);
                failedBilling.setStatus(SubscriptionBillingStatus.FAILED);
                failedBilling.setBillingDate(LocalDate.now());
                subscriptionBillingRepository.save(failedBilling);

                return new SubscriptionPaymentResponse(
                        false, failedBilling.getId(), employerId, planType.name(),
                        amount, SubscriptionBillingStatus.FAILED.name(), null,
                        "PAYMENT_NOT_PAID", "결제가 완료되지 않았습니다.");
            });
        }

        // 3. 결제 성공 확정 — SubscriptionBilling 레코드를 별도 트랜잭션에 먼저 저장.
        //    이후 BillingKey/지갑 처리가 실패해도 결제 사실은 DB에 남아 조회/조정 가능.
        final Long[] billingIdHolder = new Long[1];
        transactionTemplate.executeWithoutResult(txStatus -> {
            SubscriptionBilling billing = new SubscriptionBilling();
            billing.setEmployerId(employerId);
            billing.setPlanType(planType);
            billing.setAmount(amount);
            billing.setBillingDate(LocalDate.now());
            billing.markPaid(portonePaymentId);
            subscriptionBillingRepository.save(billing);
            billingIdHolder[0] = billing.getId();

            try {
                String invoiceUrl = paymentInvoicePdfService.generateSubscriptionInvoice(billing);
                billing.setInvoicePdfUrl(invoiceUrl);
                subscriptionBillingRepository.save(billing);
            } catch (Exception e) {
                log.error("구독 결제 인보이스 생성 실패: billingId={}, error={}",
                        billing.getId(), e.getMessage());
            }
        });

        // 4. BillingKey 갱신 및 지갑 크레딧 — 실패 시 billingId로 수동 조정 가능
        try {
            transactionTemplate.executeWithoutResult(txStatus -> {
                // 빌링키 저장 또는 업데이트
                billingKeyRepository.findByEmployerIdAndActiveTrue(employerId)
                        .ifPresent(existing -> {
                            existing.deactivate();
                            billingKeyRepository.save(existing);
                        });
                BillingKey newBillingKey = new BillingKey(employerId, billingKey, planType);
                billingKeyRepository.save(newBillingKey);

                // PLATFORM_REVENUE 지갑 크레딧 - 비관적 락 적용 및 UNIQUE 제약 예외처리로 동시성 이슈 해결
                Wallet revenueWallet = getOrCreatePlatformRevenueWallet();
                revenueWallet.credit(amount);
                walletRepository.save(revenueWallet);

                walletTransactionRepository.save(new WalletTransaction(
                        revenueWallet.getId(), TransactionType.CREDIT, amount,
                        TransactionReferenceType.SUBSCRIPTION_PAYMENT, billingIdHolder[0],
                        planType.name() + " 구독 결제 수익", revenueWallet.getBalance()));

                log.info("구독 결제 완료: employerId={}, planType={}, amount={}, billingId={}",
                        employerId, planType, amount, billingIdHolder[0]);
            });
        } catch (Exception e) {
            // BillingKey/지갑 처리 실패 — 결제 기록(billingId)은 이미 저장됐으므로 수동 조정 가능
            log.error("구독 결제 후처리 실패 (결제는 완료됨): employerId={}, billingId={}, error={}",
                    employerId, billingIdHolder[0], e.getMessage());
        }

        return new SubscriptionPaymentResponse(
                true, billingIdHolder[0], employerId, planType.name(),
                amount, SubscriptionBillingStatus.PAID.name(), LocalDate.now(),
                null, null);
    }

    /**
     * 스케줄러 전용 자동결제 메서드.
     * processPayment()와 동일한 내구성(durability) 패턴을 적용합니다:
     *   1. PENDING PaymentAttempt 저장 (별도 트랜잭션)
     *   2. PortOne API 호출 (트랜잭션 외부)
     *   3. PaymentAttempt 상태 업데이트 (별도 트랜잭션) — 결제 증거 보존
     *   4. SubscriptionBilling 저장 (별도 트랜잭션) — 결제 사실 기록
     *   5. BillingKey 갱신 + 지갑 처리 (별도 트랜잭션, try/catch)
     */
    public void chargeScheduled(BillingKey billingKey, long amount) {
        Long employerId = billingKey.getEmployerId();
        String bKey = billingKey.getBillingKey();
        if (employerId == null || bKey == null || bKey.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PlanType planType = billingKey.getPlanType();
        if (planType == null || planType == PlanType.FREE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        // 클라이언트 제공 금액 대신 서버 사이드 플랜 가격 사용 (금액 위변조 방지)
        amount = planType.getMonthlyPrice();
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 이중결제 방지: 5분 이내 동일 employer + planType 에 대한 SUCCESS 시도가 있으면 재결제 차단
        Optional<PaymentAttempt> recentSuccess = paymentAttemptRepository
                .findFirstByEmployerIdAndPlanTypeAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        employerId, planType.name(), "SUCCESS", LocalDateTime.now().minusMinutes(5));
        if (recentSuccess.isPresent()) {
            log.warn("[자동결제] 이중결제 차단: employerId={}, planType={}, recentAttemptId={}",
                    employerId, planType, recentSuccess.get().getId());
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 1. PENDING PaymentAttempt 저장 — idempotencyKey 중복 삽입으로 TOCTOU 이중결제 원자적 차단
        final String paymentId = "sub-" + UUID.randomUUID();
        final long finalAmount = amount;
        final String idempotencyKey = "sub:" + employerId + ":" + planType.name() + ":" + YearMonth.now();
        try {
            transactionTemplate.executeWithoutResult(txStatus -> {
                PaymentAttempt attempt = new PaymentAttempt();
                attempt.setId(paymentId);
                attempt.setEmployerId(employerId);
                attempt.setPlanType(planType.name());
                attempt.setStatus("PENDING");
                attempt.setIdempotencyKey(idempotencyKey);
                paymentAttemptRepository.save(attempt);
            });
        } catch (DataIntegrityViolationException e) {
            log.warn("[자동결제] 이중결제 원자적 차단 (idempotencyKey 중복): employerId={}, planType={}, key={}",
                    employerId, planType, idempotencyKey);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 2. 트랜잭션 외부에서 포트원 API 호출
        PortOnePaymentInfo paymentInfo;
        try {
            paymentInfo = portOneApiClient.chargeBillingKey(
                    paymentId, bKey,
                    finalAmount,
                    planType.name() + " 구독 자동결제",
                    "employer-" + employerId);
        } catch (Exception e) {
            transactionTemplate.executeWithoutResult(txStatus -> {
                paymentAttemptRepository.findById(paymentId).ifPresent(attempt -> {
                    attempt.setStatus("FAILED");
                    attempt.setIdempotencyKey(null); // 실패 시 키 해제 — 다음 스케줄 재시도 허용
                    paymentAttemptRepository.save(attempt);
                });
                SubscriptionBilling billing = new SubscriptionBilling();
                billing.setEmployerId(employerId);
                billing.setPlanType(planType);
                billing.setAmount(finalAmount);
                billing.setBillingDate(LocalDate.now());
                billing.setStatus(SubscriptionBillingStatus.FAILED);
                subscriptionBillingRepository.save(billing);
                log.error("[자동결제] 실패: employerId={}, error={}", employerId, e.getMessage());
            });
            throw new RuntimeException("자동결제 처리 실패: " + e.getMessage(), e);
        }

        final PortOnePaymentInfo finalPaymentInfo = paymentInfo;

        // 3. 결제 결과에 따라 PaymentAttempt 상태를 즉시 별도 트랜잭션에 저장 — 결제 증거 보존
        transactionTemplate.executeWithoutResult(txStatus -> {
            paymentAttemptRepository.findById(paymentId).ifPresent(attempt -> {
                attempt.setStatus(finalPaymentInfo.isPaid() ? "SUCCESS" : "FAILED");
                if (!finalPaymentInfo.isPaid()) attempt.setIdempotencyKey(null); // 실패 시 키 해제 — 재시도 허용
                paymentAttemptRepository.save(attempt);
            });
        });

        if (!finalPaymentInfo.isPaid()) {
            transactionTemplate.executeWithoutResult(txStatus -> {
                SubscriptionBilling billing = new SubscriptionBilling();
                billing.setEmployerId(employerId);
                billing.setPlanType(planType);
                billing.setAmount(finalAmount);
                billing.setBillingDate(LocalDate.now());
                billing.setStatus(SubscriptionBillingStatus.FAILED);
                subscriptionBillingRepository.save(billing);
                log.warn("[자동결제] 결제 미완료: employerId={}, planType={}", employerId, planType);
            });
            return;
        }

        // 4. 결제 성공 확정 — SubscriptionBilling 레코드를 별도 트랜잭션에 먼저 저장
        final Long[] billingIdHolder = new Long[1];
        transactionTemplate.executeWithoutResult(txStatus -> {
            SubscriptionBilling billing = new SubscriptionBilling();
            billing.setEmployerId(employerId);
            billing.setPlanType(planType);
            billing.setAmount(finalAmount);
            billing.setBillingDate(LocalDate.now());
            billing.markPaid(finalPaymentInfo.getPaymentId());
            subscriptionBillingRepository.save(billing);
            billingIdHolder[0] = billing.getId();

            try {
                String invoiceUrl = paymentInvoicePdfService.generateSubscriptionInvoice(billing);
                billing.setInvoicePdfUrl(invoiceUrl);
                subscriptionBillingRepository.save(billing);
            } catch (Exception e) {
                log.error("구독 결제 인보이스 생성 실패(자동결제): billingId={}, error={}",
                        billing.getId(), e.getMessage());
            }
        });

        // 5. BillingKey 갱신 + 지갑 처리 — 실패 시 billingId로 수동 조정 가능
        try {
            transactionTemplate.executeWithoutResult(txStatus -> {
                billingKey.updateNextBillingDate();
                billingKeyRepository.save(billingKey);

                Wallet revenueWallet = getOrCreatePlatformRevenueWallet();
                revenueWallet.credit(finalAmount);
                walletRepository.save(revenueWallet);

                walletTransactionRepository.save(new WalletTransaction(
                        revenueWallet.getId(), TransactionType.CREDIT, finalAmount,
                        TransactionReferenceType.SUBSCRIPTION_PAYMENT, billingIdHolder[0],
                        planType.name() + " 구독 자동결제 수익", revenueWallet.getBalance()));

                log.info("[자동결제] 완료: employerId={}, planType={}, amount={}, billingId={}",
                        employerId, planType, finalAmount, billingIdHolder[0]);
            });
        } catch (Exception e) {
            log.error("[자동결제] 후처리 실패 (결제는 완료됨): employerId={}, billingId={}, error={}",
                    employerId, billingIdHolder[0], e.getMessage());
        }
    }

    private Wallet getOrCreatePlatformRevenueWallet() {
        return walletRepository.findByWalletTypeWithLock(WalletType.PLATFORM_REVENUE)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setWalletType(WalletType.PLATFORM_REVENUE);
                    w.setBalance(0L);
                    try {
                        return walletRepository.save(w);
                    } catch (DataIntegrityViolationException ex) {
                        return walletRepository.findByWalletTypeWithLock(WalletType.PLATFORM_REVENUE)
                                .orElseThrow(() -> new BusinessException(ErrorCode.WALLET_NOT_FOUND));
                    }
                });
    }

    @Transactional(readOnly = true)
    public SubscriptionBillingItem getBillingById(Long billingId, Long requesterId) {
        SubscriptionBilling billing = subscriptionBillingRepository.findById(billingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        // 본인 결제 내역만 조회 허용
        if (!billing.getEmployerId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
        }

        return new SubscriptionBillingItem(
                billing.getId(), billing.getPlanType().name(),
                billing.getAmount(), billing.getStatus().name(),
                billing.getBillingDate(), billing.getPaidDate(),
                billing.getInvoicePdfUrl());
    }

    @Override
    public SubscriptionPaymentResult processSubscriptionPayment(
            Long employerId, String planType, long amount, String billingKey) {

        SubscriptionPaymentRequest request = new SubscriptionPaymentRequest(employerId, planType, amount, billingKey, null);
        SubscriptionPaymentResponse response = processPayment(request);

        return new SubscriptionPaymentResult(
                response.success(),
                response.billingId(),
                response.planType(),
                response.amount(),
                response.status(),
                response.errorCode(),
                response.message());
    }

    /**
     * 구독 업그레이드 결제를 1회 처리하고 결제 결과와 다음 결제일을 함께 반환합니다.
     *
     * <p>processPayment()를 통해 결제를 처리한 뒤, 저장된 BillingKey에서
     * nextBillingDate를 읽어 결합된 결과를 반환합니다.
     */

    @Override
    @Transactional
    public SubscriptionPaymentResult verifyOneTimeSubscriptionPayment(
            Long employerId, String planType, long amount, String paymentId) {

        if (employerId == null || paymentId == null || paymentId.isBlank() || planType == null || planType.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        PlanType targetPlanType;
        try {
            targetPlanType = PlanType.valueOf(planType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (targetPlanType == PlanType.FREE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        if (subscriptionBillingRepository.existsByTransactionId(paymentId)) {
            SubscriptionBilling existing = subscriptionBillingRepository.findByTransactionId(paymentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_FAILED));

            if (!existing.getEmployerId().equals(employerId)) {
                throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
            }

            return toSubscriptionPaymentResult(existing);
        }

        PortOnePaymentInfo paymentInfo = portOneApiClient.getPayment(paymentId);
        if (!paymentInfo.isPaid()) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        if (paymentInfo.getTotalAmount() != targetPlanType.getMonthlyPrice()) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        PortOnePaymentInfo.CustomDataInfo customData = paymentInfo.getCustomData();
        if (customData == null
                || customData.getEmployerId() == null
                || !employerId.equals(customData.getEmployerId())
                || customData.getPlanType() == null
                || !targetPlanType.name().equalsIgnoreCase(customData.getPlanType())) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED);
        }

        SubscriptionBilling billing = new SubscriptionBilling();
        billing.setEmployerId(employerId);
        billing.setPlanType(targetPlanType);
        billing.setAmount(targetPlanType.getMonthlyPrice());
        billing.setBillingDate(LocalDate.now());
        billing.markPaid(paymentId);
        try {
            subscriptionBillingRepository.save(billing);
        } catch (DataIntegrityViolationException e) {
            SubscriptionBilling existing = subscriptionBillingRepository.findByTransactionId(paymentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_FAILED));

            if (!existing.getEmployerId().equals(employerId)) {
                throw new BusinessException(ErrorCode.SETTLEMENT_FORBIDDEN);
            }

            return toSubscriptionPaymentResult(existing);
        }

        try {
            String invoiceUrl = paymentInvoicePdfService.generateSubscriptionInvoice(billing);
            billing.setInvoicePdfUrl(invoiceUrl);
            subscriptionBillingRepository.save(billing);
        } catch (Exception e) {
            log.error("subscription invoice generation failed: billingId={}, error={}", billing.getId(), e.getMessage());
        }

        Wallet revenueWallet = getOrCreatePlatformRevenueWallet();
        revenueWallet.credit(targetPlanType.getMonthlyPrice());
        walletRepository.save(revenueWallet);

        walletTransactionRepository.save(new WalletTransaction(
                revenueWallet.getId(),
                TransactionType.CREDIT,
                targetPlanType.getMonthlyPrice(),
                TransactionReferenceType.SUBSCRIPTION_PAYMENT,
                billing.getId(),
                targetPlanType.name() + " subscription payment revenue",
                revenueWallet.getBalance()
        ));

        return toSubscriptionPaymentResult(billing);
    }

    private SubscriptionPaymentResult toSubscriptionPaymentResult(SubscriptionBilling billing) {
        return new SubscriptionPaymentResult(
                SubscriptionBillingStatus.PAID.equals(billing.getStatus()),
                billing.getId(),
                billing.getPlanType().name(),
                billing.getAmount(),
                billing.getStatus().name(),
                null,
                null
        );
    }

    @Override
    public SubscriptionUpgradeResult processSubscriptionUpgrade(
            Long employerId, String planType, long amount, String billingKey) {

        SubscriptionPaymentRequest request = new SubscriptionPaymentRequest(employerId, planType, amount, billingKey, null);
        SubscriptionPaymentResponse response = processPayment(request);

        LocalDateTime nextBillingDate = null;
        if (response.success()) {
            // processPayment() 내부에서 BillingKey가 새로 저장되므로 조회하여 다음 결제일을 확인
            nextBillingDate = billingKeyRepository
                    .findByEmployerIdAndActiveTrue(employerId)
                    .map(bk -> bk.getNextBillingDate().atTime(9, 0))
                    .orElse(null);
        }

        return new SubscriptionUpgradeResult(
                response.success(),
                response.billingId(),
                response.planType(),
                response.amount(),
                response.status(),
                response.errorCode(),
                response.message(),
                nextBillingDate
        );
    }

    @Override
    public LocalDateTime getNextBillingDate(Long employerId) {
        return billingKeyRepository
                .findByEmployerIdAndActiveTrue(employerId)
                .map(bk -> bk.getNextBillingDate().atTime(9, 0))
                .orElse(null);
    }
}
