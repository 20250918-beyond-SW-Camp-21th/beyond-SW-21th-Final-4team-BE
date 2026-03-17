package com.fallguys.contract.entity;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_no", unique = true)
    private Long contractId;

    @Column(nullable = false)
    private String projectName;

    private Long freelancerId;
    private Long employerId;

    @Column(length = 100)
    private String relatedJobId;

    @Column(length = 100)
    private String relatedApplicationId;

    @Column(length = 100)
    private String relatedProposalId;

    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContractStatus status;

    private Long budget;
    private Double commissionRate;

    @Column(name = "payment_day")
    private Integer paymentDay;

    @Column(columnDefinition = "TEXT")
    private String contractPdfUrl;

    @Column(columnDefinition = "TEXT")
    private String signedPdfUrl;

    private LocalDateTime signedDate;

    // 표준근로계약서
    @Column(columnDefinition = "TEXT")
    private String jobDescription;

    @Column(length = 100)
    private String workLocation;

    @Column(length = 10)
    private String workStartTime;

    @Column(length = 10)
    private String workEndTime;

    @Column(length = 10)
    private String breakStartTime;

    @Column(length = 10)
    private String breakEndTime;

    private Integer workDaysPerWeek;

    @Column(length = 50)
    private String weeklyHoliday;

    // Employer Column(length = 200)
    private String employerBusinessName;

    @Column(length = 200)
    private String employerAddress;

    @Column(length = 100)
    private String employerCEO;

    // Freelancer
    @Column(length = 100)
    private String freelancerName;

    @Column(length = 200)
    private String freelancerAddress;

    @Column(length = 50)
    private String freelancerPhone;

    // Signatures (Base64 data URL)
    @Column(columnDefinition = "TEXT")
    private String employerSignature;

    private LocalDateTime employerSignedDate;

    @Column(columnDefinition = "TEXT")
    private String freelancerSignature;

    private LocalDateTime freelancerSignedDate;

    @Column(columnDefinition = "TEXT")
    private String aiLegalAdvice;


    public void signBy(String role, String signature) {
        if ("FREELANCER".equalsIgnoreCase(role)) {
            this.freelancerSignature = signature;
            this.freelancerSignedDate = LocalDateTime.now();
        } else if ("EMPLOYER".equalsIgnoreCase(role)) {
            this.employerSignature = signature;
            this.employerSignedDate = LocalDateTime.now();
        } else {
            throw new BusinessException(ErrorCode.CONTRACT_FORBIDDEN);
        }
    }

    public boolean isBothSigned() {
        return employerSignature != null && !employerSignature.isBlank()
                && freelancerSignature != null && !freelancerSignature.isBlank();
    }

    public boolean isActivatable() {
        return isBothSigned() && status == ContractStatus.WAITING_SIGNATURE;
    }

    public void activate() {
        if (!isActivatable()) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_ACTIVATABLE);
        }
        this.status = ContractStatus.IN_PROGRESS;
        this.signedDate = LocalDateTime.now();
    }

    public void complete() {
        if (status != ContractStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.CONTRACT_NOT_IN_PROGRESS);
        }
        this.status = ContractStatus.COMPLETED;
    }

    public void reject() {
        if (status != ContractStatus.WAITING_SIGNATURE) {
            throw new BusinessException(ErrorCode.CONTRACT_CANNOT_REJECT);
        }
        this.status = ContractStatus.REJECTED;
    }
}
