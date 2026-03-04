package com.fallguys.mypage.repository.resume;

import com.fallguys.mypage.entity.resume.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    
    Optional<Resume> findByFreelancerId(Long freelancerId);

}
