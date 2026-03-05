package com.fallguys.payment.service;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.api.payment.SubscriptionPaymentQuery;
import com.fallguys.common.api.payment.SubscriptionPaymentResult;
import com.fallguys.payment.api.web.dto.*;
import com.fallguys.payment.entity.*;
import com.fallguys.payment.portone.PortOneApiClient;
import com.fallguys.payment.portone.PortOnePaymentInfo;
import com.fallguys.payment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentService implements SubscriptionPaymentQuery {

    private final SubscriptionBillingRepository subscriptionBillingRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PortOneApiClient portOneApiClient;

    /**
     * 빌링키로 구독 결제 처리
     * 구독 모듈에서 billingKey를 받아 포트원으로 즉시 결제 후 SubscriptionBilling 레코드 생성
     */
    @Transactional
    public SubscriptionPaymentResponse processPayment(SubscriptionPaymentRequest request) {
        Long employerId = request.getEmployerId();
        PlanType planType;
        try {
            planType = PlanType.valueOf(request.getPlanType());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        long amount = request.getAmount();
        String billingKey = request.getBillingKey();

        // 빌링키로 포트원 결제 호출
        PortOnePaymentInfo paymentInfo;
        try {
            paymentInfo = portOneApiClient.chargeBillingKey(
                    billingKey, amount,
                    planType.name() + " 구독 결제",
                    "employer-" + employerId);
        } catch (BusinessException e) {
            // 결제 실패 시 FAILED 레코드 저장
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
        }

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
        // markPaid()가 status=PAID, paidDate, transactionId를 한 번에 처리하므로 중복 setter 제거
        SubscriptionBilling billing = new SubscriptionBilling();
        billing.setEmployerId(employerId);
        billing.setPlanType(planType);
        billing.setAmount(amount);
        billing.setBillingDate(LocalDate.now());
        billing.markPaid(paymentInfo.getPaymentId());
        subscriptionBillingRepository.save(billing);

        // PLATFORM_REVENUE 지갑 크레딧
        Wallet revenueWallet = walletRepository.findByWalletType(WalletType.PLATFORM_REVENUE)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setWalletType(WalletType.PLATFORM_REVENUE);
                    w.setBalance(0L);
                    return walletRepository.save(w);
                });
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
    }

    /**
     * 스케줄러 전용 자동결제 메서드
     * BillingKey 엔티티를 변경하지 않고 PortOne에 결제 요청만 합니다.
     * 테스트 모드: v2_test_ API Secret 사용 시 실제 결제 없이 처리됩니다.
     */
    @Transactional
    public void chargeScheduled(BillingKey billingKey, long amount) {
        Long employerId = billingKey.getEmployerId();
        PlanType planType = billingKey.getPlanType();

        SubscriptionBilling billing = new SubscriptionBilling();
        billing.setEmployerId(employerId);
        billing.setPlanType(planType);
        billing.setAmount(amount);
        billing.setBillingDate(LocalDate.now());

        try {
            PortOnePaymentInfo paymentInfo = portOneApiClient.chargeBillingKey(
                    billingKey.getBillingKey(),
                    amount,
                    planType.name() + " 구독 자동결제",
                    "employer-" + employerId);

            if (!paymentInfo.isPaid()) {
                billing.setStatus(SubscriptionBillingStatus.FAILED);
                subscriptionBillingRepository.save(billing);
                log.warn("[자동결제] 결제 미완료: employerId={}, planType={}", employerId, planType);
                return;
            }

            billing.markPaid(paymentInfo.getPaymentId()); // status=PAID, paidDate, transactionId 일괄 설정
            subscriptionBillingRepository.save(billing);

            billingKey.updateNextBillingDate();
            billingKeyRepository.save(billingKey);

            // PLATFORM_REVENUE 지갑 크레딧
            Wallet revenueWallet = walletRepository.findByWalletType(WalletType.PLATFORM_REVENUE)
                    .orElseGet(() -> {
                        Wallet w = new Wallet();
                        w.setWalletType(WalletType.PLATFORM_REVENUE);
                        w.setBalance(0L);
                        return walletRepository.save(w);
                    });
            revenueWallet.credit(amount);
            walletRepository.save(revenueWallet);

            walletTransactionRepository.save(new WalletTransaction(
                    revenueWallet.getId(), TransactionType.CREDIT, amount,
                    TransactionReferenceType.SUBSCRIPTION_PAYMENT, billing.getId(),
                    planType.name() + " 구독 자동결제 수익", revenueWallet.getBalance()));

            log.info("[자동결제] 완료: employerId={}, planType={}, amount={}, billingId={}",
                    employerId, planType, amount, billing.getId());

        } catch (BusinessException e) {
            billing.setStatus(SubscriptionBillingStatus.FAILED);
            subscriptionBillingRepository.save(billing);
            log.error("[자동결제] 실패: employerId={}, error={}", employerId, e.getMessage());
            throw e; // 스케줄러에서 개별 실패 로깅을 위해 재던짐
        }
    }

    @Transactional(readOnly = true)
    public SubscriptionBillingItem getBillingById(Long billingId) {
        SubscriptionBilling billing = subscriptionBillingRepository.findById(billingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTLEMENT_NOT_FOUND));

        return new SubscriptionBillingItem(
                billing.getId(), billing.getPlanType().name(),
                billing.getAmount(), billing.getStatus().name(),
                billing.getBillingDate(), billing.getPaidDate());
    }

    /**
     * 타 모듈용 구독 결제 처리 (SubscriptionPaymentQuery 구현)
     * 멤버십 변경 시 subscription 모듈에서 호출합니다.
     */
    @Override
    @Transactional
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
}