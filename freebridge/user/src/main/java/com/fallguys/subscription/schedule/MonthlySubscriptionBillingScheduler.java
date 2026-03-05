package com.fallguys.subscription.schedule;

import com.fallguys.mypage.entity.employer.Employer;
import com.fallguys.mypage.entity.employer.Subscription;
import com.fallguys.mypage.repository.employer.EmployerRepository;
import com.fallguys.subscription.api.shared.ExternalPaymentPort;
import com.fallguys.subscription.entity.PlanGrade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlySubscriptionBillingScheduler {

    private final EmployerRepository employerRepository;
    private final ExternalPaymentPort externalPaymentPort;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void processMonthlyBilling() {
        LocalDateTime now = LocalDateTime.now();
        int page = 0;
        int pageSize = 200;
        int maxPageRetries = 1;
        boolean stopOnPageFailure = false;

        while (true) {
            Pageable pageable = PageRequest.of(page, pageSize);
            Page<Employer> employerPage = employerRepository
                    .findByNextBillingDateLessThanEqualAndBillingKeyIsNotNull(now, pageable);

            if (employerPage.isEmpty()) {
                return;
            }

            boolean pageProcessed = false;
            int attempt = 0;
            while (!pageProcessed && attempt <= maxPageRetries) {
                try {
                    processEmployers(employerPage.getContent(), now);
                    pageProcessed = true;
                } catch (Exception ex) {
                    attempt++;
                    log.warn("[MonthlyBilling] page processing failed page={}, attempt={}, msg={}",
                            page, attempt, ex.getMessage(), ex);
                    if (attempt > maxPageRetries) {
                        if (stopOnPageFailure) {
                            return;
                        }
                        break;
                    }
                }
            }

            if (employerPage.isLast()) {
                return;
            }
            page++;
        }
    }

    private void processEmployers(List<Employer> employers, LocalDateTime now) {
        for (Employer employer : employers) {
            if (employer.getSubscription() == Subscription.BASIC) {
                continue;
            }

            LocalDateTime nextBillingDate = employer.getNextBillingDate();
            if (nextBillingDate == null || nextBillingDate.isAfter(now)) {
                continue;
            }

            // If cancellation is pending (BASIC), skip payment and apply.
            if (employer.getPendingSubscription() == Subscription.BASIC
                    && employer.getPlanChangeEffectiveDate() != null
                    && !employer.getPlanChangeEffectiveDate().isAfter(now)) {
                employer.applyPendingSubscription();
                employer.updateNextBillingDate(null);
                continue;
            }

            PlanGrade planGrade = toPlanGrade(employer.getSubscription());
            ExternalPaymentPort.PaymentResult result = externalPaymentPort.requestSubscriptionPayment(
                    employer.getUserId(),
                    planGrade.name(),
                    planGrade.getMonthlyPrice(),
                    employer.getBillingKey()
            );

            if (!result.success()) {
                log.warn("[MonthlyBilling] payment failed userId={}, code={}, msg={}",
                        employer.getUserId(), result.errorCode(), result.errorMessage());
                continue;
            }

            employer.applyPendingSubscription();
            employer.updateNextBillingDate(nextBillingDate.plusMonths(1));
        }
    }

    private PlanGrade toPlanGrade(Subscription subscription) {
        return switch (subscription) {
            case BASIC -> PlanGrade.BASIC;
            case PRO -> PlanGrade.PRO;
            case PRIME -> PlanGrade.PRIME;
        };
    }
}
