package com.fallguys.matchs.repository;

import com.fallguys.matchs.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalRepo extends JpaRepository<Proposal,Long> {
    List<Proposal> findAllByEmployerIdOrderByCreatedAtDesc(Long employerId);

    List<Proposal> findAllByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);
}
