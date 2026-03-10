package com.fallguys.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "freelancer_settlements")
public class FreelancerSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long contractId;

    // 1:1 with EmployerSettlement
    @Column(nullable = false, unique = true)
    private Long employerSettlementId;

    @Column(nullable = false)
    private Long freelancerId;

    // Gross amount = EmployerSettlement.billingAmount
    @Column(nullable = false)
    private Long totalAmount;

    // floor(totalAmount * commissionRate)
    @Column(nullable = false)
    private Long platformFee;

    // floor((totalAmount - platformFee) * 0.033)
    @Column(nullable = false)
    private Long tax;

    // totalAmount - platformFee - tax
    @Column(nullable = false)
    private Long netAmount;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private FreelancerSettlementStatus status;

    // paymentDay date for this installment month — when auto-disbursement fires
    @Column(nullable = false)
    private LocalDate scheduledDate;

    // Set by the auto-disbursement scheduler
    private LocalDate paidDate;

    @Column(columnDefinition = "TEXT")
    private String receiptPdfUrl;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void markPaid() {
        this.status = FreelancerSettlementStatus.PAID;
        this.paidDate = LocalDate.now();
    }

    // public void cancel() {
    //     this.status = FreelancerSettlementStatus.CANCELLED;
    // }

    public boolean isPending() {
        return this.status == FreelancerSettlementStatus.PENDING;
    }
}
