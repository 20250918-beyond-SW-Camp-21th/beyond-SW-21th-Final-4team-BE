package com.fallguys.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// Immutable ledger entry — never updated or deleted after creation
@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private TransactionType type;

    // Always positive
    @Column(nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TransactionReferenceType referenceType;

    // ID of the related record (e.g. EmployerSettlement.id, SubscriptionBilling.id)
    private Long referenceId;

    @Column(length = 200)
    private String description;

    // Wallet balance snapshot immediately after this transaction
    @Column(nullable = false)
    private Long balanceAfter;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public WalletTransaction(Long walletId, TransactionType type, Long amount,
                              TransactionReferenceType referenceType, Long referenceId,
                              String description, Long balanceAfter) {
        this.walletId = walletId;
        this.type = type;
        this.amount = amount;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.description = description;
        this.balanceAfter = balanceAfter;
    }
}
