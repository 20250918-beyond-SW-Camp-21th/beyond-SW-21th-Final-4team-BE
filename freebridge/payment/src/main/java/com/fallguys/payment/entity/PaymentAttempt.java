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

    /**
     * Period-scoped deduplication key (e.g. "pay:42:PREMIUM:2026-03").
     * Unique index provides the atomic guard against TOCTOU double-charge races;
     * the soft 5-minute query check is kept as an early short-circuit before the DB write.
     * Nullable to preserve compatibility with rows created before this column was added.
     */
    @Column(name = "idempotency_key", unique = true, nullable = true)
    private String idempotencyKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
