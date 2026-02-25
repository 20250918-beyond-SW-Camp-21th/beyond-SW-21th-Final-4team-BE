package com.fallguys.recruitment.repository;

import com.fallguys.recruitment.api.dto.response.JobPostingSearchDTO;
import com.fallguys.recruitment.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepo extends JpaRepository<JobPosting, Long> {
    List<JobPostingSearchDTO> findAllByEmployerIdAndDeletedFalse(Long EmployerId);
}
