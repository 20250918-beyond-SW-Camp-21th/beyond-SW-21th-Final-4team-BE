package com.fallguys.contract.api.web.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ContractResponse {

    private Long id;
    private Long contractId;       // Display ID (e.g. 1001)
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
    private Double commissionRate;
    private Integer paymentDay;
    private String contractPdfUrl;
    private String signedPdfUrl;
    private LocalDateTime signedDate;
    private String aiLegalAdvice;

    // 표준근로계약서 fields
    private String jobDescription;
    private String workLocation;
    private String workStartTime;
    private String workEndTime;
    private String breakStartTime;
    private String breakEndTime;
    private Integer workDaysPerWeek;
    private String weeklyHoliday;

    private String employerBusinessName;
    private String employerAddress;
    private String employerCEO;

    private String freelancerAddress;
    private String freelancerPhone;

    private String employerSignature;
    private LocalDateTime employerSignedDate;
    private String freelancerSignature;
    private LocalDateTime freelancerSignedDate;
    private Boolean employerSigned;
    private Boolean freelancerSigned;

    // TODO: Replace mock values with real names once user module is ready
    private String freelancerName;
    private String employerName;
}
