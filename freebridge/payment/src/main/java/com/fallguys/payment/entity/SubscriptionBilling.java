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
@Table(name = "subscription_billings")
public class SubscriptionBilling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employerId;

    // Plan the employer is upgrading to
    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private PlanType planType;

    // SubscriptionPlan.monthlyPrice for the target plan
    @Column(nullable = false)
    private Long amount;

    // PortOne imp_uid for this payment
    @Column(length = 100, unique = true)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private SubscriptionBillingStatus status;

    @Column(nullable = false)
    private LocalDate billingDate;

    // Set on successful PortOne verification
    private LocalDate paidDate;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String invoicePdfUrl;

    public void markPaid(String transactionId) {
        this.status = SubscriptionBillingStatus.PAID;
        this.paidDate = LocalDate.now();
        this.transactionId = transactionId;
    }

    public void markFailed() {
        this.status = SubscriptionBillingStatus.FAILED;
    }
}
