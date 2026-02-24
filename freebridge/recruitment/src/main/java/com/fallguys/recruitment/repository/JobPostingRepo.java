package com.fallguys.recruitment.repository;

import com.fallguys.recruitment.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobPostingRepo extends JpaRepository<JobPosting, Long> {

}
