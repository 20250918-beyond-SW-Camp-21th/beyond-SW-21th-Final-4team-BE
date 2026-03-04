package com.fallguys.payment.repository;

import com.fallguys.payment.entity.BillingKey;
import com.fallguys.payment.entity.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingKeyRepository extends JpaRepository<BillingKey, Long> {

    Optional<BillingKey> findByEmployerIdAndActiveTrue(Long employerId);

    Optional<BillingKey> findByEmployerIdAndPlanTypeAndActiveTrue(Long employerId, PlanType planType);

    List<BillingKey> findByActiveTrue();

    List<BillingKey> findByActiveTrueAndNextBillingDateLessThanEqual(java.time.LocalDate date);
}
