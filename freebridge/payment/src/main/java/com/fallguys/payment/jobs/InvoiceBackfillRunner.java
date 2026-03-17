package com.fallguys.payment.jobs;

import com.fallguys.common.api.contract.ContractInfo;
import com.fallguys.common.api.contract.ContractQuery;
import com.fallguys.payment.entity.EmployerSettlement;
import com.fallguys.payment.entity.EmployerSettlementStatus;
import com.fallguys.payment.entity.SubscriptionBilling;
import com.fallguys.payment.entity.SubscriptionBillingStatus;
import com.fallguys.payment.repository.EmployerSettlementRepository;
import com.fallguys.payment.repository.SubscriptionBillingRepository;
import com.fallguys.payment.service.PaymentInvoicePdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile("backfill-invoices")
@RequiredArgsConstructor
public class InvoiceBackfillRunner implements ApplicationRunner {

    private final EmployerSettlementRepository employerSettlementRepository;
    private final SubscriptionBillingRepository subscriptionBillingRepository;
    private final PaymentInvoicePdfService paymentInvoicePdfService;
    private final ContractQuery contractQuery;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        int serviceFeeCreated = 0;
        int serviceFeeSkipped = 0;
        int subscriptionCreated = 0;
        int subscriptionSkipped = 0;

        List<EmployerSettlement> settlements = employerSettlementRepository.findByInvoicePdfUrlIsNull();
        for (EmployerSettlement settlement : settlements) {
            if (settlement.getStatus() != EmployerSettlementStatus.PAID
                    && settlement.getStatus() != EmployerSettlementStatus.DISBURSED) {
                serviceFeeSkipped++;
                continue;
            }
            try {
                ContractInfo contract = contractQuery.getContractInfoByContractId(settlement.getContractId());
                String url = paymentInvoicePdfService.generateServiceFeeInvoice(settlement, contract);
                settlement.setInvoicePdfUrl(url);
                employerSettlementRepository.save(settlement);
                serviceFeeCreated++;
            } catch (Exception e) {
                log.error("서비스 수수료 인보이스 백필 실패: settlementId={}, error={}",
                        settlement.getId(), e.getMessage());
            }
        }

        List<SubscriptionBilling> billings = subscriptionBillingRepository
                .findByInvoicePdfUrlIsNullAndStatus(SubscriptionBillingStatus.PAID);
        for (SubscriptionBilling billing : billings) {
            try {
                String url = paymentInvoicePdfService.generateSubscriptionInvoice(billing);
                billing.setInvoicePdfUrl(url);
                subscriptionBillingRepository.save(billing);
                subscriptionCreated++;
            } catch (Exception e) {
                log.error("구독 인보이스 백필 실패: billingId={}, error={}", billing.getId(), e.getMessage());
            }
        }

        log.info("Invoice backfill done. serviceFeeCreated={}, serviceFeeSkipped={}, subscriptionCreated={}, subscriptionSkipped={}",
                serviceFeeCreated, serviceFeeSkipped, subscriptionCreated, subscriptionSkipped);

        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }
}
