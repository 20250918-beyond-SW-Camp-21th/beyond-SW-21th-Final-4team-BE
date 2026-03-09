package com.fallguys.payment.repository;

import com.fallguys.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, String> {

    /**
     * Finds the most recent SUCCESS attempt for a given employer + planType within a time window.
     * Uses Spring Data's findFirst naming convention to enforce LIMIT 1 and avoid
     * IncorrectResultSizeDataAccessException when multiple SUCCESS rows exist.
     */
    Optional<PaymentAttempt> findFirstByEmployerIdAndPlanTypeAndStatusAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long employerId, String planType, String status, LocalDateTime since);
}
