package com.fallguys.contract.api.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
public class CreateContractRequest {

    @NotBlank(message = "프로젝트명은 필수입니다.")
    private String projectName;

    @NotNull(message = "프리랜서 ID는 필수입니다.")
    private Long freelancerId;

    private String freelancerName;     // 계약서 PDF에 표시될 프리랜서 이름

    @Size(max = 100, message = "relatedJobId는 100자를 초과할 수 없습니다.")
    private String relatedJobId;

    @Size(max = 100, message = "relatedApplicationId는 100자를 초과할 수 없습니다.")
    private String relatedApplicationId;

    @Size(max = 100, message = "relatedProposalId는 100자를 초과할 수 없습니다.")
    private String relatedProposalId;

    @NotNull(message = "계약 시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "계약 종료일은 필수입니다.")
    private LocalDate endDate;

    @NotNull(message = "급여는 필수입니다.")
    private Long budget;

    @NotNull(message = "급여 지급일은 필수입니다.")
    private Integer paymentDay;

    // 표준근로계약서용
    @NotBlank(message = "업무 내용은 필수입니다.")
    private String jobDescription;

    @NotBlank(message = "근무 장소는 필수입니다.")
    private String workLocation;     // default "원격근무"

    @NotBlank(message = "근무 시작 시간은 필수입니다.")
    private String workStartTime;    // "09:00" or "자율"

    @NotBlank(message = "근무 종료 시간은 필수입니다.")
    private String workEndTime;

    @NotBlank(message = "휴게 시작 시간은 필수입니다.")
    private String breakStartTime;

    @NotBlank(message = "휴게 종료 시간은 필수입니다.")
    private String breakEndTime;

    @NotNull(message = "주 근무일 수는 필수입니다.")
    private Integer workDaysPerWeek;

    @NotBlank(message = "주휴일은 필수입니다.")
    private String weeklyHoliday;    // "토, 일"

    @NotBlank(message = "고용주 사업체명은 필수입니다.")
    private String employerBusinessName;

    @NotBlank(message = "고용주 주소는 필수입니다.")
    private String employerAddress;

    @NotBlank(message = "고용주 대표자명은 필수입니다.")
    private String employerCEO;

    private String freelancerAddress;  // 프리랜서가 서명 시 직접 입력

    private String freelancerPhone;    // 프리랜서가 서명 시 직접 입력

    private String employerSignature;  // Base64 data URL (optional at creation)

}
