package com.fallguys.appmain.adapter;

import com.fallguys.contract.api.shared.ContractActivatedEvent;
import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.repository.ContractRepository;
import com.fallguys.recruitment.service.JobPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractActivationJobPostingListener {

    private final ContractRepository contractRepository;
    private final JobPostingService jobPostingService;

    @EventListener
    public void handle(ContractActivatedEvent event) {
        contractRepository.findById(event.getContractId()).ifPresent(this::closeRelatedJobPosting);
    }

    private void closeRelatedJobPosting(Contract contract) {
        String relatedJobId = contract.getRelatedJobId();
        if (relatedJobId == null || relatedJobId.isBlank()) {
            return;
        }

        try {
            jobPostingService.closeJobPosting(Long.parseLong(relatedJobId.trim()));
        } catch (NumberFormatException e) {
            log.warn("계약 활성화 후 공고 마감 처리 실패: relatedJobId is not numeric. contractId={}, relatedJobId={}",
                    contract.getContractId(), relatedJobId);
        } catch (RuntimeException e) {
            log.warn("계약 활성화 후 공고 마감 처리 실패: contractId={}, relatedJobId={}",
                    contract.getContractId(), relatedJobId, e);
        }
    }
}
