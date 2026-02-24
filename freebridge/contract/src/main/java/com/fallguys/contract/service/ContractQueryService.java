package com.fallguys.contract.service;

import com.fallguys.contract.api.shared.ContractInfo;
import com.fallguys.contract.api.shared.ContractQuery;
import com.fallguys.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractQueryService implements ContractQuery {

    private final ContractRepository contractRepository;

    @Override
    public ContractInfo getContractInfo(Long contractId) {
        var c = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found: " + contractId));

        return new ContractInfo(
                c.getId(),
                c.getContractId(),
                c.getProjectName(),
                c.getFreelancerId(),
                c.getEmployerId(),
                c.getCommissionRate(),
                c.getPaymentDay(),
                c.getStartDate(),
                c.getEndDate(),
                c.getBudget()
        );
    }
}