package com.fallguys.matchs.repository;

import com.fallguys.matchs.entity.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalRepo extends JpaRepository<Proposal,Long> {
    Page<Proposal> findAllByEmployerIdOrderByCreatedAtDesc(Long employerId, Pageable pageable);

    Page<Proposal> findAllByFreelancerIdOrderByCreatedAtDesc(Long freelancerId, Pageable pageable);

    boolean existsByJobPostingIdAndFreelancerId(Long jobPostingId, Long freelancerId);

    long countByFreelancerId(Long freelancerId);
}
