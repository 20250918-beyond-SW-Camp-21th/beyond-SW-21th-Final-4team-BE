package com.fallguys.recruitment.repository;

import com.fallguys.recruitment.entity.JobPostingFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingFavoriteRepo extends JpaRepository<JobPostingFavorite, Long> {
    List<JobPostingFavorite> findAllByFreelancerId(Long freelancerId);

    boolean existsByFreelancerIdAndJobPostingId(Long freelancerId, Long jobPostingId);

    void deleteByFreelancerIdAndJobPostingId(Long freelancerId, Long jobPostingId);
}
