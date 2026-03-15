package com.fallguys.common.api.contract;

public interface ContractQuery {

    boolean existsContract(Long contractId);

    ContractInfo getContractInfo(Long contractId);
}
