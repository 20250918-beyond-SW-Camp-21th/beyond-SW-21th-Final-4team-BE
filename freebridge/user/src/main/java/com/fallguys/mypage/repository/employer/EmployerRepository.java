package com.fallguys.mypage.repository.employer;

import com.fallguys.mypage.entity.employer.Employer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployerRepository extends JpaRepository<Employer, Long> {
    
    Optional<Employer> findByUserId(Long userId);
    
}
