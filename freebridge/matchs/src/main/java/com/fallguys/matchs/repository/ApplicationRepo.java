package com.fallguys.matchs.repository;

import com.fallguys.matchs.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepo extends JpaRepository<Application,Long> {
}
