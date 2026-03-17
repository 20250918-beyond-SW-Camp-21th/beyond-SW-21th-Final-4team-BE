package com.fallguys.contract.repository;

import com.fallguys.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    boolean existsByContractId(Long contractId);

    Optional<Contract> findByContractId(Long contractId);

    List<Contract> findByEmployerIdOrderByIdDesc(Long employerId);

    List<Contract> findByFreelancerIdOrderByIdDesc(Long freelancerId);
}
