package com.fallguys.userlike.repository;

import com.fallguys.userlike.entity.EmployerFreelancerFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployerFreelancerFavoriteRepository extends JpaRepository<EmployerFreelancerFavorite, Long> {

    void deleteByEmployerIdAndFreelancerId(Long employerId, Long freelancerId);
}
