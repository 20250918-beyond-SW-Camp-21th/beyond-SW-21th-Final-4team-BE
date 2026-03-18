package com.fallguys.common.api.contract;

import java.time.LocalDate;

public record ContractInfo(
        Long id,
        Long contractId,
        String projectName,
        Long freelancerId,
        Long employerId,
        String status,
        boolean employerSigned,
        boolean freelancerSigned,
        Double commissionRate,
        Integer paymentDay,
        LocalDate startDate,
        LocalDate endDate,
        Long budget,
        String employerBusinessName) {
}
