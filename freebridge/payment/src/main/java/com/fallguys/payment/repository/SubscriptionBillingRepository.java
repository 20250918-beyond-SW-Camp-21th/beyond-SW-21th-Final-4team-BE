package com.fallguys.payment.repository;

import com.fallguys.payment.entity.SubscriptionBilling;
import com.fallguys.payment.entity.SubscriptionBillingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionBillingRepository extends JpaRepository<SubscriptionBilling, Long> {

    Page<SubscriptionBilling> findByEmployerId(Long employerId, Pageable pageable);

    Page<SubscriptionBilling> findByEmployerIdAndStatus(Long employerId, SubscriptionBillingStatus status, Pageable pageable);

    boolean existsByTransactionId(String transactionId);

    Optional<SubscriptionBilling> findByTransactionId(String transactionId);

    java.util.List<SubscriptionBilling> findByInvoicePdfUrlIsNullAndStatus(SubscriptionBillingStatus status);
}
