package com.fallguys.contract.service;

import com.fallguys.common.api.contract.ContractInfo;
import com.fallguys.common.api.contract.ContractQuery;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.contract.entity.Contract;
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
    public boolean existsContract(Long contractId) {
        return contractRepository.existsByContractId(contractId);
    }

    /**
     * 내부 PK(id)로만 계약 정보를 조회한다.
     */
    @Override
    public ContractInfo getContractInfoById(Long id) {
        Contract c = contractRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND));

        return toContractInfo(c);
    }

    /**
     * 비즈니스 계약번호(contractId)로만 계약 정보를 조회한다.
     */
    @Override
    public ContractInfo getContractInfoByContractId(Long contractId) {
        Contract c = contractRepository.findByContractId(contractId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTRACT_NOT_FOUND));

        return toContractInfo(c);
    }

    private ContractInfo toContractInfo(Contract c) {
        return new ContractInfo(
                c.getId(),
                c.getContractId(),
                c.getProjectName(),
                c.getFreelancerId(),
                c.getEmployerId(),
                c.getStatus() != null ? c.getStatus().name() : null,
                hasSignature(c.getEmployerSignature()),
                hasSignature(c.getFreelancerSignature()),
                c.getCommissionRate(),
                c.getPaymentDay(),
                c.getStartDate(),
                c.getEndDate(),
                c.getBudget(),
                c.getEmployerBusinessName());
    }

    private boolean hasSignature(String signature) {
        return signature != null && !signature.isBlank();
    }
}
