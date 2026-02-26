package com.fallguys.matchs.repository;

import com.fallguys.matchs.entity.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepo extends JpaRepository<Proposal,Long> {
    Page<Proposal> findAllByEmployerIdOrderByCreatedAtDesc(Long employerId, Pageable pageable);

    Page<Proposal> findAllByFreelancerIdOrderByCreatedAtDesc(Long freelancerId, Pageable pageable);
}
