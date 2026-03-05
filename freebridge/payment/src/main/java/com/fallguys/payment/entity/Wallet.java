package com.fallguys.payment.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "wallets", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "owner_id", "wallet_type" })
})
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User ID for EMPLOYER/FREELANCER wallets; null for PLATFORM_ESCROW /
    // PLATFORM_REVENUE
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private WalletType walletType;

    // Current balance in KRW (always >= 0 for platform wallets)
    @Column(nullable = false)
    private Long balance;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public void credit(long amount) {
        this.balance += amount;
    }

    public void debit(long amount) {
        this.balance -= amount;
    }

    @PrePersist
    @PreUpdate
    public void ensureSentinelOwnerId() {
        if (walletType == WalletType.PLATFORM_ESCROW || walletType == WalletType.PLATFORM_REVENUE) {
            this.ownerId = 0L;
        }
    }
}
