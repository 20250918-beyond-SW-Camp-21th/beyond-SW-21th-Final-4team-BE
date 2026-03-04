package com.fallguys.payment.scheduler;

import com.fallguys.payment.config.PortOneProperties;
import com.fallguys.payment.entity.BillingKey;
import com.fallguys.payment.entity.PlanType;
import com.fallguys.payment.repository.BillingKeyRepository;
import com.fallguys.payment.service.AdminSettlementService;
import com.fallguys.payment.service.SubscriptionPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 정산 및 구독 자동결제 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final AdminSettlementService adminSettlementService;
    private final SubscriptionPaymentService subscriptionPaymentService;
    private final BillingKeyRepository billingKeyRepository;
    private final PortOneProperties portOneProperties;

    /**
     * 매일 오전 9시: 지급 예정일이 도래한 프리랜서 정산 자동 처리
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void runDailyDisbursement() {
        log.info("[스케줄러] 프리랜서 정산 자동 지급 시작");
        try {
            adminSettlementService.runDisbursement();
        } catch (Exception e) {
            log.error("[스케줄러] 정산 자동 지급 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 매일 자정: 활성 빌링키 보유 고용주 중 결제일이 도래한 대상에게 구독 자동결제
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void runMonthlySubscriptionBilling() {
        log.info("[스케줄러] 구독 자동결제 시작");

        java.time.LocalDate today = java.time.LocalDate.now();
        List<BillingKey> activeBillingKeys = billingKeyRepository
                .findByActiveTrueAndNextBillingDateLessThanEqual(today);
        if (activeBillingKeys.isEmpty()) {
            log.info("[스케줄러] 활성 빌링키 없음. 구독 자동결제 건너뜀.");
            return;
        }

        int successCount = 0;
        for (BillingKey bk : activeBillingKeys) {
            try {
                long amount = resolvePlanPrice(bk.getPlanType());
                if (amount <= 0) {
                    log.warn("[스케줄러] 무료 플랜 빌링키 건너뜀: employerId={}", bk.getEmployerId());
                    continue;
                }

                // chargeScheduled: BillingKey 엔티티를 새로 만들지 않고 결제만 수행
                // (processPayment는 최초 구독 시에만 사용, 스케줄러는 chargeScheduled 사용)
                subscriptionPaymentService.chargeScheduled(bk, amount);
                successCount++;
                log.debug("[스케줄러] 구독 자동결제 성공: employerId={}, planType={}",
                        bk.getEmployerId(), bk.getPlanType());
            } catch (Exception e) {
                log.error("[스케줄러] 구독 자동결제 실패: employerId={}, error={}",
                        bk.getEmployerId(), e.getMessage(), e);
            }
        }
        log.info("[스케줄러] 구독 자동결제 완료: 성공={}/{}", successCount, activeBillingKeys.size());
    }

    private long resolvePlanPrice(PlanType planType) {
        return switch (planType) {
            case PRO -> portOneProperties.getSubscription().getPlan().getPro();
            case PRIME -> portOneProperties.getSubscription().getPlan().getPrime();
            default -> 0L;
        };
    }
}
