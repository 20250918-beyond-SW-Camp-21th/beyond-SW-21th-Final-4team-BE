package com.fallguys.payment.repository;

import com.fallguys.payment.entity.FreelancerSettlement;
import com.fallguys.payment.entity.FreelancerSettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FreelancerSettlementRepository extends JpaRepository<FreelancerSettlement, Long> {

    Optional<FreelancerSettlement> findByEmployerSettlementId(Long employerSettlementId);

    List<FreelancerSettlement> findByStatusAndScheduledDateLessThanEqual(
            FreelancerSettlementStatus status, LocalDate scheduledDate);

    Page<FreelancerSettlement> findByFreelancerId(Long freelancerId, Pageable pageable);

    Page<FreelancerSettlement> findByFreelancerIdAndStatus(Long freelancerId, FreelancerSettlementStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(f.netAmount), 0) FROM FreelancerSettlement f " +
            "WHERE f.freelancerId = :freelancerId AND f.status = 'PENDING'")
    Long sumNetAmountByFreelancerIdAndStatusPending(@Param("freelancerId") Long freelancerId);

    @Query("SELECT COALESCE(SUM(f.netAmount), 0) FROM FreelancerSettlement f " +
            "WHERE f.freelancerId = :freelancerId AND f.status = 'PAID'")
    Long sumNetAmountByFreelancerIdAndStatusPaid(@Param("freelancerId") Long freelancerId);

    @Query("SELECT COUNT(f) FROM FreelancerSettlement f WHERE f.freelancerId = :freelancerId AND f.status = :status")
    Integer countByFreelancerIdAndStatus(@Param("freelancerId") Long freelancerId, @Param("status") FreelancerSettlementStatus status);
}
