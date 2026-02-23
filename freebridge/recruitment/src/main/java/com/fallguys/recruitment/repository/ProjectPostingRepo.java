package com.fallguys.recruitment.repository;

import com.fallguys.recruitment.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectPostingRepo extends JpaRepository<Project, String> {
}
