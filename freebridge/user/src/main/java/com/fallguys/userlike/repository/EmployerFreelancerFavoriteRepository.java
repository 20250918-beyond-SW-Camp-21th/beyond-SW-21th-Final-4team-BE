package com.fallguys.userlike.repository;

import com.fallguys.userlike.entity.EmployerFreelancerFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployerFreelancerFavoriteRepository extends JpaRepository<EmployerFreelancerFavorite, Long> {

    boolean existsByEmployerIdAndFreelancerId(Long employerId, Long freelancerId);

    Optional<EmployerFreelancerFavorite> findByEmployerIdAndFreelancerId(Long employerId, Long freelancerId);

    List<EmployerFreelancerFavorite> findAllByEmployerIdOrderByCreatedAtDesc(Long employerId);

    void deleteByEmployerIdAndFreelancerId(Long employerId, Long freelancerId);
}
