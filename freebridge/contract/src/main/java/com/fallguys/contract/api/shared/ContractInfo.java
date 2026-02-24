package com.fallguys.contract.api.shared;

import java.time.LocalDate;

public record ContractInfo(
        Long id,
        Long contractId,
        String projectName,
        Long freelancerId,
        Long employerId,
        Double commissionRate,
        Integer paymentDay,
        LocalDate startDate,
        LocalDate endDate,
        Long budget
) {}