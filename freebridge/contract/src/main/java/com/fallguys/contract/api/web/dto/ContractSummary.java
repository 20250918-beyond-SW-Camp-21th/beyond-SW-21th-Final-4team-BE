package com.fallguys.contract.api.web.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

// Used in contract list responses. Lighter than ContractResponse — no signatures or work schedule fields.
@Getter
@Builder
public class ContractSummary {

    private Long id;
    private Long contractId;
    private String projectName;
    private Long freelancerId;
    private Long employerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long budget;

    // Computed from signature presence — not stored separately
    private Boolean employerSigned;
    private Boolean freelancerSigned;

    // TODO: Replace mock values with real names once user module is ready
    private String freelancerName;
    private String employerName;
}