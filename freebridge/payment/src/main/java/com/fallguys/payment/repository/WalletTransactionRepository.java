package com.fallguys.payment.repository;

import com.fallguys.payment.entity.TransactionReferenceType;
import com.fallguys.payment.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletId(Long walletId, Pageable pageable);

    Page<WalletTransaction> findByWalletIdAndReferenceType(
            Long walletId, TransactionReferenceType referenceType, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t " +
            "WHERE t.walletId = :walletId AND t.type = 'DEBIT'")
    Long sumDebitByWalletId(@Param("walletId") Long walletId);

    @Query("SELECT COUNT(t) FROM WalletTransaction t WHERE t.walletId = :walletId")
    Integer countByWalletId(@Param("walletId") Long walletId);
}
