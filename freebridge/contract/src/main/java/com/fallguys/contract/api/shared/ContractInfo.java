package com.fallguys.contract.api.shared;

import java.time.LocalDate;

// Record DTO exposed to other modules via api/shared.
// Used by settlement module to get contract data for settlement generation.
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