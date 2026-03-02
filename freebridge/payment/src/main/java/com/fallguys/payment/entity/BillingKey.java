package com.fallguys.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "billing_keys")
public class BillingKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employerId;

    @Column(nullable = false, length = 200)
    private String billingKey;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private PlanType planType;

    @Column(nullable = false)
    private boolean active;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public BillingKey(Long employerId, String billingKey, PlanType planType) {
        this.employerId = employerId;
        this.billingKey = billingKey;
        this.planType = planType;
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
