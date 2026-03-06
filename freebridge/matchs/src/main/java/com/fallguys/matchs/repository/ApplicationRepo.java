package com.fallguys.matchs.repository;

import com.fallguys.matchs.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ApplicationRepo extends JpaRepository<Application,Long> {
    Page<Application> findAllByEmployerIdOrderByCreatedAtDesc(Long employerId, Pageable pageable);

    Page<Application> findAllByFreelancerIdOrderByCreatedAtDesc(Long freelancerId, Pageable pageable);

    List<Application> findAllByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    List<Application> findAllByJobPostingIdOrderByCreatedAtDesc(Long jobPostingId);

    long countByJobPostingId(Long jobPostingId);

    @Query("""
            select a.jobPostingId as jobPostingId, count(a.id) as applicantCount
            from Application a
            where a.jobPostingId in :jobPostingIds
            group by a.jobPostingId
            """)
    List<JobPostingApplicantCountProjection> countApplicantsByJobPostingIds(@Param("jobPostingIds") Collection<Long> jobPostingIds);

    interface JobPostingApplicantCountProjection {
        Long getJobPostingId();
        Long getApplicantCount();
    }
}
