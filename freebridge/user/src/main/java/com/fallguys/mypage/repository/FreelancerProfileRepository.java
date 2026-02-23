package com.fallguys.mypage.repository;

import com.fallguys.mypage.entity.freelancer.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FreelancerProfileRepository extends JpaRepository<Freelancer, UUID> {
    Optional<Freelancer> findByUserId(UUID userId);
}
