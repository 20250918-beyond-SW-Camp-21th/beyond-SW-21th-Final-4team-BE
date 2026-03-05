package com.fallguys.mypage.schedule;

import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployerSubscriptionScheduler {

    private final EmployerRepository employerRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void applyScheduledSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Employer> employersToUpdate = employerRepository
                .findByPendingSubscriptionIsNotNullAndPlanChangeEffectiveDateLessThanEqual(now);

        if (employersToUpdate.isEmpty()) {
            return;
        }

        int successCount = 0;
        for (Employer employer : employersToUpdate) {
            try {
                if (employer.getPendingSubscription() == Subscription.BASIC
                        && employer.getNextBillingDate() == null) {
                    employer.applyPendingSubscription();
                    employer.updateNextBillingDate(null);
                    successCount++;
                } else if (employer.getPendingSubscription() != Subscription.BASIC
                        && employer.getNextBillingDate() == null) {
                    log.warn("[Scheduler] pending non-BASIC without nextBillingDate userId={}, pending={}",
                            employer.getUserId(), employer.getPendingSubscription());
                }
            } catch (Exception e) {
                log.error("[Scheduler] pending subscription apply failed userId={}, msg={}",
                        employer.getUserId(), e.getMessage());
            }
        }

        log.info("[Scheduler] pending BASIC applied count={}", successCount);
    }
}
