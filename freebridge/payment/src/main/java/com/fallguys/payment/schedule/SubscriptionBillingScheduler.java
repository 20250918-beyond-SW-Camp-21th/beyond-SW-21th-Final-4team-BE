package com.fallguys.payment.schedule;

import com.fallguys.payment.entity.BillingKey;
import com.fallguys.payment.entity.PlanType;
import com.fallguys.payment.repository.BillingKeyRepository;
import com.fallguys.payment.service.SubscriptionPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 구독 정기결제 스케줄러 — 매일 09:00 실행
 *
 * <p>active=true 이고 nextBillingDate &lt;= 오늘인 BillingKey를 조회하여
 * PortOne 빌링키 재결제를 수행합니다.
 * 결제 성공 시 BillingKey의 nextBillingDate가 한 달 뒤로 갱신됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionBillingScheduler {

    private final BillingKeyRepository billingKeyRepository;
    private final SubscriptionPaymentService subscriptionPaymentService;

    @Scheduled(cron = "0 0 9 * * *")
    public void processDueSubscriptions() {
        LocalDate today = LocalDate.now();

        List<BillingKey> dueBillingKeys = billingKeyRepository
                .findByActiveTrueAndNextBillingDateLessThanEqual(today);

        if (dueBillingKeys.isEmpty()) {
            log.info("[SubscriptionScheduler] 결제 대상 없음 (기준일: {})", today);
            return;
        }

        log.info("[SubscriptionScheduler] 결제 대상: {}건 (기준일: {})", dueBillingKeys.size(), today);

        int successCount = 0;
        for (BillingKey billingKey : dueBillingKeys) {
            try {
                // FREE 플랜은 결제 불필요
                if (billingKey.getPlanType() == PlanType.FREE) {
                    log.warn("[SubscriptionScheduler] FREE 플랜 BillingKey 스킵: employerId={}",
                            billingKey.getEmployerId());
                    continue;
                }

                long amount = billingKey.getPlanType().getMonthlyPrice();
                subscriptionPaymentService.chargeScheduled(billingKey, amount);
                successCount++;

            } catch (Exception e) {
                log.error("[SubscriptionScheduler] 결제 실패: employerId={}, planType={}, error={}",
                        billingKey.getEmployerId(), billingKey.getPlanType(), e.getMessage());
            }
        }

        log.info("[SubscriptionScheduler] 완료: 성공={}/{}", successCount, dueBillingKeys.size());
    }
}
