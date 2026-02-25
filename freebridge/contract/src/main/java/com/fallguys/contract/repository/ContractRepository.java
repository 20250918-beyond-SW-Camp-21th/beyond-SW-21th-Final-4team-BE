package com.fallguys.contract.repository;

import com.fallguys.contract.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    List<Contract> findByEmployerIdOrderByIdDesc(Long employerId);

    List<Contract> findByFreelancerIdOrderByIdDesc(Long freelancerId);
}