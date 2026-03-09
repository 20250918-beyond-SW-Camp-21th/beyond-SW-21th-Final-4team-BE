package com.fallguys.payment.repository;

import com.fallguys.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, String> {

    /**
     * Finds the most recent SUCCESS attempt for a given employer + planType within a time window.
     * Used for idempotency: if a SUCCESS already exists recently, skip re-charging.
     */
    @Query("SELECT p FROM PaymentAttempt p " +
           "WHERE p.employerId = :employerId " +
           "  AND p.planType = :planType " +
           "  AND p.status = 'SUCCESS' " +
           "  AND p.createdAt >= :since " +
           "ORDER BY p.createdAt DESC")
    Optional<PaymentAttempt> findRecentSuccess(
            @Param("employerId") Long employerId,
            @Param("planType") String planType,
            @Param("since") LocalDateTime since);
}
