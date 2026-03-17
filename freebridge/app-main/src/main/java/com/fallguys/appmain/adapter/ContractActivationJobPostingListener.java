package com.fallguys.appmain.adapter;

import com.fallguys.contract.api.shared.ContractActivatedEvent;
import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.repository.ContractRepository;
import com.fallguys.recruitment.service.JobPostingService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractActivationJobPostingListener {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final ContractRepository contractRepository;
    private final JobPostingService jobPostingService;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    @EventListener
    public void handle(ContractActivatedEvent event) {
        contractRepository.findById(event.getContractId()).ifPresent(this::closeRelatedJobPosting);
    }

    @Async("taskExecutor")
    @EventListener
    public void handleRetry(ContractActivationJobPostingRetryEvent event) {
        meterRegistry.counter("contract.activation.job_posting.retry.attempts").increment();

        try {
            jobPostingService.closeJobPosting(parseJobPostingId(event.relatedJobId()));
            log.info(
                    "계약 활성화 후 공고 마감 재시도 성공: contractId={}, relatedJobId={}, attempt={}",
                    event.contractId(),
                    event.relatedJobId(),
                    event.attempt()
            );
        } catch (RuntimeException e) {
            if (event.attempt() >= MAX_RETRY_ATTEMPTS) {
                meterRegistry.counter("contract.activation.job_posting.retry.exhausted").increment();
                log.error(
                        "계약 활성화 후 공고 마감 재시도 소진: contractId={}, relatedJobId={}, attempt={}, previousError={}, currentError={}",
                        event.contractId(),
                        event.relatedJobId(),
                        event.attempt(),
                        event.errorMessage(),
                        e.getMessage(),
                        e
                );
                return;
            }

            publishRetryEvent(event.contractId(), event.relatedJobId(), e, event.attempt() + 1);
        }
    }

    private void closeRelatedJobPosting(Contract contract) {
        String relatedJobId = contract.getRelatedJobId();
        if (relatedJobId == null || relatedJobId.isBlank()) {
            return;
        }

        try {
            jobPostingService.closeJobPosting(parseJobPostingId(relatedJobId));
        } catch (NumberFormatException e) {
            meterRegistry.counter("contract.activation.job_posting.invalid_related_job_id").increment();
            log.warn("계약 활성화 후 공고 마감 처리 실패: relatedJobId is not numeric. contractId={}, relatedJobId={}",
                    contract.getContractId(), relatedJobId);
        } catch (RuntimeException e) {
            meterRegistry.counter("contract.activation.job_posting.retry.scheduled").increment();
            log.warn("계약 활성화 후 공고 마감 처리 실패: contractId={}, relatedJobId={}",
                    contract.getContractId(), relatedJobId, e);
            publishRetryEvent(contract.getContractId(), relatedJobId, e, 1);
        }
    }

    private Long parseJobPostingId(String relatedJobId) {
        return Long.parseLong(relatedJobId.trim());
    }

    private void publishRetryEvent(Long contractId, String relatedJobId, RuntimeException e, int attempt) {
        log.warn(
                "계약 활성화 후 공고 마감 재시도 예약: contractId={}, relatedJobId={}, attempt={}, error={}",
                contractId,
                relatedJobId,
                attempt,
                e.getMessage(),
                e
        );
        eventPublisher.publishEvent(new ContractActivationJobPostingRetryEvent(
                contractId,
                relatedJobId,
                e.getMessage(),
                attempt
        ));
    }
}
