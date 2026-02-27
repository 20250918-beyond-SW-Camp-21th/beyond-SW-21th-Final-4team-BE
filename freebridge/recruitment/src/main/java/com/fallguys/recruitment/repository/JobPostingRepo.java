package com.fallguys.recruitment.repository;

import com.fallguys.recruitment.entity.JobPostingStatus;
import com.fallguys.recruitment.entity.Status;
import com.fallguys.recruitment.entity.JobPosting;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepo extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findAllByEmployerIdAndStatusNot(Long employerId, Status status);

    List<JobPosting> findAllByStatusNot(Status status);

    List<JobPosting> findAllByStatusAndPostingStatusIn(Status status, Collection<JobPostingStatus> postingStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from JobPosting j where j.id = :jobPostingId")
    Optional<JobPosting> findByIdForUpdate(@Param("jobPostingId") Long jobPostingId);
}
