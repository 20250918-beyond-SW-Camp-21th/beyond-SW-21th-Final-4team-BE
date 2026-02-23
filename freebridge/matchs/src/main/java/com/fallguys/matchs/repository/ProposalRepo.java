package com.fallguys.matchs.repository;

import com.fallguys.matchs.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepo extends JpaRepository<Proposal,Long> {
}
