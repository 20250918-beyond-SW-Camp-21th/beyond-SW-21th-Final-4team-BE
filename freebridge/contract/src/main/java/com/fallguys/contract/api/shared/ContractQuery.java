package com.fallguys.contract.api.shared;

// Interface exposed to other modules via api/shared.
// Other modules inject this — never ContractRepository or Contract entity directly.
public interface ContractQuery {

    ContractInfo getContractInfo(Long contractId);
}