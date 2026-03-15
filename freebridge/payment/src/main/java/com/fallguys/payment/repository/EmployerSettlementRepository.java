package com.fallguys.payment.repository;

import com.fallguys.payment.entity.EmployerSettlement;
import com.fallguys.payment.entity.EmployerSettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface EmployerSettlementRepository extends JpaRepository<EmployerSettlement, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT e FROM EmployerSettlement e WHERE e.id = :id")
        Optional<EmployerSettlement> findByIdWithLock(@Param("id") Long id);

        boolean existsByTransactionId(String transactionId);

        Optional<EmployerSettlement> findByTransactionId(String transactionId);

        List<EmployerSettlement> findByContractId(Long contractId);

        Page<EmployerSettlement> findByEmployerId(Long employerId, Pageable pageable);

        Page<EmployerSettlement> findByEmployerIdAndStatus(Long employerId, EmployerSettlementStatus status,
                        Pageable pageable);

        @Query("SELECT e FROM EmployerSettlement e WHERE e.employerId = :employerId " +
                        "AND e.dueDate BETWEEN :from AND :to")
        Page<EmployerSettlement> findByEmployerIdAndDueDateBetween(
                        @Param("employerId") Long employerId,
                        @Param("from") LocalDate from,
                        @Param("to") LocalDate to,
                        Pageable pageable);

        @Query("SELECT COALESCE(SUM(e.totalPayment), 0) FROM EmployerSettlement e " +
                        "WHERE e.employerId = :employerId AND e.status = 'PAID'")
        Long sumTotalPaymentByEmployerIdAndStatusPaid(@Param("employerId") Long employerId);

        @Query("SELECT COALESCE(SUM(e.totalPayment), 0) FROM EmployerSettlement e " +
                        "WHERE e.employerId = :employerId AND e.status = 'DISBURSED'")
        Long sumTotalPaymentByEmployerIdAndStatusDisbursed(@Param("employerId") Long employerId);

        @Query("SELECT COUNT(e) FROM EmployerSettlement e WHERE e.employerId = :employerId AND e.status = :status")
        Integer countByEmployerIdAndStatus(@Param("employerId") Long employerId,
                        @Param("status") EmployerSettlementStatus status);

        @Query("SELECT e FROM EmployerSettlement e WHERE e.employerId = :employerId " +
                        "AND e.status = 'PAID' ORDER BY e.dueDate ASC")
        List<EmployerSettlement> findFirstPaidByEmployerId(@Param("employerId") Long employerId, Pageable pageable);

        Page<EmployerSettlement> findAll(Pageable pageable);

        Page<EmployerSettlement> findByStatus(EmployerSettlementStatus status, Pageable pageable);

        List<EmployerSettlement> findByInvoicePdfUrlIsNull();
}
