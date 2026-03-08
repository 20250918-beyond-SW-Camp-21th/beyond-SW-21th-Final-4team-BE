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
@Table(name = "employer_settlements", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "contract_id", "installment_number" })
})
public class EmployerSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long contractId;

    @Column(nullable = false)
    private Long employerId;

    @Column(nullable = false)
    private Long freelancerId;

    // PortOne imp_uid — shared across all installments of the same contract
    @Column(length = 100)
    private String transactionId;

    // Installment amount ≈ budget / totalMonths (last installment absorbs
    // remainder)
    @Column(nullable = false)
    private Long billingAmount;

    // billingAmount * commissionRate (floored)
    @Column(nullable = false)
    private Long platformFee;

    // billingAmount + platformFee
    @Column(nullable = false)
    private Long totalPayment;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private EmployerSettlementStatus status;

    @Column(columnDefinition = "TEXT")
    private String invoicePdfUrl;

    // Scheduled disbursement date — derived from contract paymentDay for this
    // installment month
    @Column(nullable = false)
    private LocalDate dueDate;

    // Set at contract signing when upfront PortOne payment is confirmed
    private LocalDate paidDate;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void markPaid(String transactionId) {
        this.status = EmployerSettlementStatus.PAID;
        this.paidDate = LocalDate.now();
        this.transactionId = transactionId;
    }

    public void markDisbursed() {
        this.status = EmployerSettlementStatus.DISBURSED;
    }

    // public void cancel() {
    //     this.status = EmployerSettlementStatus.CANCELLED;
    // }
}
