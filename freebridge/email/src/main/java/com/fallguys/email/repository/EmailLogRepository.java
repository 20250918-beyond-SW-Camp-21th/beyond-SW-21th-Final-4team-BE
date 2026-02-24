package com.fallguys.email.repository;

import com.fallguys.email.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    long deleteBySentAtBefore(LocalDateTime dateTime);
}
