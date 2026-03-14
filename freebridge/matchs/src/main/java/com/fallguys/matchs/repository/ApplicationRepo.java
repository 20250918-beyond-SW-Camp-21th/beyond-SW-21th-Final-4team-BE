package com.fallguys.matchs.repository;

import com.fallguys.matchs.entity.Application;
import com.fallguys.matchs.entity.MatchsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepo extends JpaRepository<Application,Long> {
    Page<Application> findAllByEmployerIdOrderByCreatedAtDesc(Long employerId, Pageable pageable);

    Page<Application> findAllByFreelancerIdOrderByCreatedAtDesc(Long freelancerId, Pageable pageable);

    boolean existsByJobPostingIdAndFreelancerId(Long jobPostingId, Long freelancerId);

    List<Application> findAllByJobPostingIdOrderByCreatedAtDesc(Long jobPostingId);

    long countByJobPostingId(Long jobPostingId);

    long countByFreelancerId(Long freelancerId);

    @Query("""
            select a.jobPostingId as jobPostingId, count(a.id) as applicantCount
            from Application a
            where a.jobPostingId in :jobPostingIds
            group by a.jobPostingId
            """)
    List<JobPostingApplicantCountProjection> countApplicantsByJobPostingIds(@Param("jobPostingIds") Collection<Long> jobPostingIds);

    @Query("""
            select a.id as applicationId, a.jobPostingId as jobPostingId, a.freelancerId as freelancerId, a.status as status
            from Application a
            where a.id = :applicationId
            """)
    Optional<ApplicantStatusProjection> findApplicantStatusProjectionById(@Param("applicationId") Long applicationId);

    interface JobPostingApplicantCountProjection {
        Long getJobPostingId();
        Long getApplicantCount();
    }

    interface ApplicantStatusProjection {
        Long getApplicationId();
        Long getJobPostingId();
        Long getFreelancerId();
        MatchsStatus getStatus();
    }
}
