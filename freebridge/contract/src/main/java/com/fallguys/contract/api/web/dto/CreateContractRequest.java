package com.fallguys.contract.api.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CreateContractRequest {

    private String projectName;
    private Long freelancerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long budget;
    private Integer paymentDay;      // 10, 15, 25, or 31

    // 표준근로계약서용
    private String jobDescription;
    private String workLocation;     // default "원격근무"
    private String workStartTime;    // "09:00" or "자율"
    private String workEndTime;
    private String breakStartTime;
    private String breakEndTime;
    private Integer workDaysPerWeek;
    private String weeklyHoliday;    // "토, 일"

    private String employerBusinessName;
    private String employerAddress;
    private String employerCEO;

    private String freelancerAddress;
    private String freelancerPhone;

    private String employerSignature;  // Base64 data URL
}