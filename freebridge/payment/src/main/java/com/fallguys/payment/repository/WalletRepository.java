package com.fallguys.payment.repository;

import com.fallguys.payment.entity.Wallet;
import com.fallguys.payment.entity.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByWalletType(WalletType walletType);

    Optional<Wallet> findByOwnerIdAndWalletType(Long ownerId, WalletType walletType);
}
