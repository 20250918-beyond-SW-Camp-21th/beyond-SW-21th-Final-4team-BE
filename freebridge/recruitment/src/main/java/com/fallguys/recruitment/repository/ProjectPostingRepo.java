package com.fallguys.recruitment.repository;

import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectPostingRepo extends JpaRepository<Project, Long> {
    boolean existsByJobPostingIdAndFreelancerId(Long jobPostingId, Long freelancerId);

    Optional<Project> findByJobPostingIdAndFreelancerId(Long jobPostingId, Long freelancerId);

    List<Project> findAllByEmployerIdOrderByCreatedAtDesc(Long employerId);

    List<Project> findAllByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    Page<Project> findAllByJobPostingIdOrderByCreatedAtDesc(Long jobPostingId, Pageable pageable);

    long countByFreelancerIdAndStatus(Long freelancerId, ProjectStatus status);
}
