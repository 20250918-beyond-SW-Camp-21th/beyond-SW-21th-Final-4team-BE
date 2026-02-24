package com.fallguys.contract.api.shared;

import org.springframework.context.ApplicationEvent;

// Published when both parties have signed and contract becomes IN_PROGRESS.
// Settlement module listens for this event to auto-generate settlement records.
public class ContractActivatedEvent extends ApplicationEvent {

    private final Long contractId;

    public ContractActivatedEvent(Object source, Long contractId) {
        super(source);
        this.contractId = contractId;
    }

    public Long getContractId() {
        return contractId;
    }
}