package com.fallguys.mypage.repository.freelancer;

import com.fallguys.mypage.entity.freelancer.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FreelancerRepository extends JpaRepository<Freelancer, Long> {

    Optional<Freelancer> findByUserId(Long userId);

}
