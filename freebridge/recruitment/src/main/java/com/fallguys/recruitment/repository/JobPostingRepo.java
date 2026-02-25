package com.fallguys.recruitment.repository;

import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepo extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findAllByEmployerIdAndStatusNot(Long employerId, Status status);

    List<JobPosting> findAllByStatusNot(Status status);

    List<JobPosting> findAllByStatusAndPostingStatus(Status status, JobPostingStatus postingStatus);
}
