package com.fallguys.contract.api.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CreateContractRequest {

    // Basic info
    private String projectName;
    private Long freelancerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long budget;
    private Integer paymentDay;      // 10, 15, 25, or 31

    // 표준근로계약서 fields
    private String jobDescription;
    private String workLocation;     // default "원격근무"
    private String workStartTime;    // "09:00" or "자율"
    private String workEndTime;
    private String breakStartTime;
    private String breakEndTime;
    private Integer workDaysPerWeek;
    private String weeklyHoliday;    // "토, 일"

    // Employer info (snapshot at creation time)
    private String employerBusinessName;
    private String employerAddress;
    private String employerCEO;

    // Freelancer info (optional)
    private String freelancerAddress;
    private String freelancerPhone;

    // Employer signs on creation
    private String employerSignature;  // Base64 data URL
}