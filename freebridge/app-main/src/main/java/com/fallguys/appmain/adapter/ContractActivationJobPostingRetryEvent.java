package com.fallguys.appmain.adapter;

public record ContractActivationJobPostingRetryEvent(
        Long contractId,
        String relatedJobId,
        String errorMessage,
        int attempt
) {
}
