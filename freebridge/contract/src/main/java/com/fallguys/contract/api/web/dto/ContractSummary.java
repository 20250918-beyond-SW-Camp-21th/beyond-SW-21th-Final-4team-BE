package com.fallguys.contract.api.web.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ContractSummary {

    private Long id;
    private Long contractId;
    private String projectName;
    private Long freelancerId;
    private Long employerId;
    private String relatedJobId;
    private String relatedApplicationId;
    private String relatedProposalId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Long budget;

    private Boolean employerSigned;
    private Boolean freelancerSigned;

    // TODO: 유저 모듈 완성되면 수정하기
    private String freelancerName;
    private String employerName;
}
