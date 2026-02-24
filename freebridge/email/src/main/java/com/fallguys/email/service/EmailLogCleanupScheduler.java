package com.fallguys.email.service;

import com.fallguys.email.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailLogCleanupScheduler {

    private final EmailLogRepository emailLogRepository;

    // 매일 새벽 3시에 30일이 지난 이메일 로그 삭제
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldEmailLogs() {
        LocalDateTime retentionThreshold = LocalDateTime.now().minusDays(30);
        log.info("이메일 로그 데이터 보존 정책 실행 - 기준일: {} 이전 데이터 삭제", retentionThreshold);

        // deleteBySentAtBefore(retentionThreshold) 호출을 위해 Repository에 메서드 추가 필요
        long deletedCount = emailLogRepository.deleteBySentAtBefore(retentionThreshold);

        log.info("이메일 로그 삭제 완료 - 삭제 건수: {}", deletedCount);
    }
}
