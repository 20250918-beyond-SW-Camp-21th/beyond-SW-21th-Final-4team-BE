package com.fallguys.payment.repository;

import com.fallguys.payment.entity.Wallet;
import com.fallguys.payment.entity.WalletType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByWalletType(WalletType walletType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.walletType = :walletType ORDER BY w.id ASC LIMIT 1")
    Optional<Wallet> findByWalletTypeWithLock(@Param("walletType") WalletType walletType);

    Optional<Wallet> findByOwnerIdAndWalletType(Long ownerId, WalletType walletType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.ownerId = :ownerId AND w.walletType = :walletType")
    Optional<Wallet> findByOwnerIdAndWalletTypeWithLock(@Param("ownerId") Long ownerId,
            @Param("walletType") WalletType walletType);
}
