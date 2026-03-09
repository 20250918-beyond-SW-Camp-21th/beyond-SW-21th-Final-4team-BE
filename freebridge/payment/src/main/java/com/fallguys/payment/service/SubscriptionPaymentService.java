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

        // 1. 트랜잭션 외부에서 빌링키로 포트원 결제 호출 (외부 API 호출이 트랜잭션에 묶이지 않도록)
        String paymentId = "sub-" + UUID.randomUUID();
        transactionTemplate.executeWithoutResult(status -> {
            PaymentAttempt attempt = new PaymentAttempt();
            attempt.setId(paymentId);
            attempt.setEmployerId(employerId);
            attempt.setPlanType(planType.name());
            attempt.setStatus("PENDING");
            paymentAttemptRepository.save(attempt);
        });

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

        // 2. 외부 API 호출 후 트랜잭션 내부에서 DB 업데이트 처리
        return transactionTemplate.execute(status -> {

            paymentAttemptRepository.findById(paymentId).ifPresent(attempt -> {
                attempt.setStatus(paymentInfo.isPaid() ? "SUCCESS" : "FAILED");
                paymentAttemptRepository.save(attempt);
            });

            if (!paymentInfo.isPaid()) {
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
            }

            // 빌링키 저장 또는 업데이트
            billingKeyRepository.findByEmployerIdAndActiveTrue(employerId)
                    .ifPresent(existing -> {
                        existing.deactivate();
                        billingKeyRepository.save(existing);
                    });
            BillingKey newBillingKey = new BillingKey(employerId, billingKey, planType);
            billingKeyRepository.save(newBillingKey);

            // SubscriptionBilling 레코드 생성
            SubscriptionBilling billing = new SubscriptionBilling();
            billing.setEmployerId(employerId);
            billing.setPlanType(planType);
            billing.setAmount(amount);
            billing.setBillingDate(LocalDate.now());
            billing.markPaid(paymentInfo.getPaymentId());
            subscriptionBillingRepository.save(billing);

            // PLATFORM_REVENUE 지갑 크레딧 - 비관적 락 적용 및 UNIQUE 제약 예외처리로 동시성 이슈 해결
            Wallet revenueWallet = getOrCreatePlatformRevenueWallet();
            revenueWallet.credit(amount);
            walletRepository.save(revenueWallet);

            walletTransactionRepository.save(new WalletTransaction(
                    revenueWallet.getId(), TransactionType.CREDIT, amount,
                    TransactionReferenceType.SUBSCRIPTION_PAYMENT, billing.getId(),
                    planType.name() + " 구독 결제 수익", revenueWallet.getBalance()));

            log.info("구독 결제 완료: employerId={}, planType={}, amount={}, billingId={}",
                    employerId, planType, amount, billing.getId());

            return new SubscriptionPaymentResponse(
                    true, billing.getId(), employerId, planType.name(),
                    amount, SubscriptionBillingStatus.PAID.name(), LocalDate.now(),
                    null, null);
        });
    }

    /**
     * 스케줄러 전용 자동결제 메서드
     * BillingKey 엔티티를 변경하지 않고 PortOne에 결제 요청만 합니다.
     * 외부 호출을 트랜잭션 외부에서 실행합니다.
     */
    public void chargeScheduled(BillingKey billingKey, long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Long employerId = billingKey.getEmployerId();
        String bKey = billingKey.getBillingKey();
        if (employerId == null || bKey == null || bKey.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        PlanType planType = billingKey.getPlanType();

        String paymentId = "sub-" + UUID.randomUUID();
        transactionTemplate.executeWithoutResult(status -> {
            PaymentAttempt attempt = new PaymentAttempt();
            attempt.setId(paymentId);
            attempt.setEmployerId(employerId);
            attempt.setPlanType(planType.name());
            attempt.setStatus("PENDING");
            paymentAttemptRepository.save(attempt);
        });

        // 트랜잭션 외부에서 API 호출
        PortOnePaymentInfo paymentInfo = null;
        try {
            paymentInfo = portOneApiClient.chargeBillingKey(
                    paymentId, bKey,
                    amount,
                    planType.name() + " 구독 자동결제",
                    "employer-" + employerId);
        } catch (Exception e) {
            // BusinessException 및 기타 모든 예외(네트워크 오류, RuntimeException 등) 동일하게 처리
            transactionTemplate.executeWithoutResult(status -> {
                paymentAttemptRepository.findById(paymentId).ifPresent(attempt -> {
                    attempt.setStatus("FAILED");
                    paymentAttemptRepository.save(attempt);
                });
                SubscriptionBilling billing = new SubscriptionBilling();
                billing.setEmployerId(employerId);
                billing.setPlanType(planType);
                billing.setAmount(amount);
                billing.setBillingDate(LocalDate.now());
                billing.setStatus(SubscriptionBillingStatus.FAILED);
                subscriptionBillingRepository.save(billing);
                log.error("[자동결제] 실패: employerId={}, error={}", employerId, e.getMessage());
            });
            throw new RuntimeException("자동결제 처리 실패: " + e.getMessage(), e);
        }

        final PortOnePaymentInfo finalPaymentInfo = paymentInfo;

        transactionTemplate.executeWithoutResult(status -> {
            paymentAttemptRepository.findById(paymentId).ifPresent(attempt -> {
                attempt.setStatus(finalPaymentInfo.isPaid() ? "SUCCESS" : "FAILED");
                paymentAttemptRepository.save(attempt);
            });

            SubscriptionBilling billing = new SubscriptionBilling();
            billing.setEmployerId(employerId);
            billing.setPlanType(planType);
            billing.setAmount(amount);
            billing.setBillingDate(LocalDate.now());

            if (!finalPaymentInfo.isPaid()) {
                billing.setStatus(SubscriptionBillingStatus.FAILED);
                subscriptionBillingRepository.save(billing);
                log.warn("[자동결제] 결제 미완료: employerId={}, planType={}", employerId, planType);
                return;
            }

            billing.markPaid(finalPaymentInfo.getPaymentId());
            subscriptionBillingRepository.save(billing);

            billingKey.updateNextBillingDate();
            billingKeyRepository.save(billingKey);

            // PLATFORM_REVENUE 지갑 크레딧 - 비관적 락 적용 및 UNIQUE 제약 예외처리로 동시성 이슈 해결
            Wallet revenueWallet = getOrCreatePlatformRevenueWallet();
            revenueWallet.credit(amount);
            walletRepository.save(revenueWallet);

            walletTransactionRepository.save(new WalletTransaction(
                    revenueWallet.getId(), TransactionType.CREDIT, amount,
                    TransactionReferenceType.SUBSCRIPTION_PAYMENT, billing.getId(),
                    planType.name() + " 구독 자동결제 수익", revenueWallet.getBalance()));

            log.info("[자동결제] 완료: employerId={}, planType={}, amount={}, billingId={}",
                    employerId, planType, amount, billing.getId());
        });
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
                billing.getBillingDate(), billing.getPaidDate());
    }

    @Override
    public SubscriptionPaymentResult processSubscriptionPayment(
            Long employerId, String planType, long amount, String billingKey) {

        SubscriptionPaymentRequest request = new SubscriptionPaymentRequest(employerId, planType, amount, billingKey);
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
    public SubscriptionUpgradeResult processSubscriptionUpgrade(
            Long employerId, String planType, long amount, String billingKey) {

        SubscriptionPaymentRequest request = new SubscriptionPaymentRequest(employerId, planType, amount, billingKey);
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
}
