package com.fallguys.recruitment.repository;

import com.fallguys.recruitment.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectPostingRepo extends JpaRepository<Project, Long> {
}
