package com.fallguys.common.api.contract;

public interface ContractQuery {

    boolean existsContract(Long contractId);

    /**
     * 내부 PK(id)로 계약 정보를 조회한다.
     */
    ContractInfo getContractInfoById(Long id);

    /**
     * 비즈니스 계약번호(contractId)로 계약 정보를 조회한다.
     */
    ContractInfo getContractInfoByContractId(Long contractId);
}
