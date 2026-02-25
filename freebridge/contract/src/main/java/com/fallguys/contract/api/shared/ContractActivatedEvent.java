package com.fallguys.contract.api.shared;

import org.springframework.context.ApplicationEvent;

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