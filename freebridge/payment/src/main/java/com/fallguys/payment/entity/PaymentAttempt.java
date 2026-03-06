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
@Table(name = "payment_attempts")
public class PaymentAttempt {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id; // UUID or generated ID for the attempt

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "plan_type", nullable = false)
    private String planType;

    @Column(name = "status", nullable = false)
    private String status; // e.g., PENDING, SUCCESS, FAILED

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
