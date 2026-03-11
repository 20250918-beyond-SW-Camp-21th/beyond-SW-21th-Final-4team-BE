package com.fallguys.review.repository;

import com.fallguys.review.entity.EmployerRejectionReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployerRejectionReasonRepository extends JpaRepository<EmployerRejectionReason, Long> {

    Page<EmployerRejectionReason> findAllByEmployerIdOrderByCreatedAtDesc(Long employerId, Pageable pageable);

    Page<EmployerRejectionReason> findAllByEmployerIdAndProjectTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            Long employerId,
            String projectTitle,
            Pageable pageable
    );

    Page<EmployerRejectionReason> findAllByFreelancerIdOrderByCreatedAtDesc(Long freelancerId, Pageable pageable);

    Page<EmployerRejectionReason> findAllByFreelancerIdAndProjectTitleContainingIgnoreCaseOrderByCreatedAtDesc(
            Long freelancerId,
            String projectTitle,
            Pageable pageable
    );
}
