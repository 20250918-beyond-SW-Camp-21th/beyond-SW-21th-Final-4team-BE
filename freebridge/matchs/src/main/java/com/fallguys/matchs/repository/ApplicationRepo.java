package com.fallguys.matchs.repository;

import com.fallguys.matchs.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepo extends JpaRepository<Application,Long> {
    List<Application> findAllByEmployerIdOrderByCreatedAtDesc(Long employerId);

    List<Application> findAllByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);
}
