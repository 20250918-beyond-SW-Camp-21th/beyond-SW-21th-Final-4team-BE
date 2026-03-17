package com.fallguys.contract.service;

import com.fallguys.common.api.contract.ContractInfo;
import com.fallguys.common.api.contract.ContractQuery;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractQueryService implements ContractQuery {

    private final ContractRepository contractRepository;

    @Override
    public boolean existsContract(Long contractId) {
        return contractRepository.existsByContractId(contractId);
    }

    @Override 
    public ContractInfo getContractInfo(Long contractId) {
        Contract c = contractRepository.findById(contractId)
                .orElseGet(() -> contractRepository.findByContractId(contractId)
                        .map(contract -> {
                            log.warn("Fallback business contractId lookup detected in ContractQuery.getContractInfo: requestedId={}, resolvedInternalId={}",
                                    contractId, contract.getId());
                            return contract;
                        })
                        .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND)));

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
                c.getBudget(),
                c.getEmployerBusinessName());
    }
}
