package com.fallguys.recruitment.repository;

import com.fallguys.recruitment.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPostingRepo extends JpaRepository<JobPosting, Long> {

}
