package com.fallguys.recruitment.service;

import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.port.ProjectExternalApi;
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class ProjectExternalApiImpl implements ProjectExternalApi {

    private final ProjectPostingRepo projectPostingRepo;
    private final RecommendationEngine recommendationEngine;
    private final Executor taskExecutor;

    public ProjectExternalApiImpl(
            ProjectPostingRepo projectPostingRepo,
            RecommendationEngine recommendationEngine,
            @Qualifier("taskExecutor") Executor taskExecutor){
        this.projectPostingRepo = projectPostingRepo;
        this.recommendationEngine = recommendationEngine;
        this.taskExecutor = taskExecutor;
    }

    @Override
    @Transactional
    public void completeProjectWithReview(ProjectCompletionData data) {
        // 1. 프로젝트 상태 완료 처리
        Project project = projectPostingRepo.findById(data.projectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        
        project.complete();
        
        // 2. AI 서버 동기화 (트랜잭션 커밋 후 비동기 실행)
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(() -> syncToAiServer(data), taskExecutor);
                }
            });
        } else {
            // 트랜잭션이 없는 경우 바로 비동기 실행
            CompletableFuture.runAsync(() -> syncToAiServer(data), taskExecutor);
        }
    }

    private void syncToAiServer(ProjectCompletionData data) {
        try {
            // AI 서버로 전송할 데이터 구성 (JSON 문자열 등으로 변환하거나 필요한 필드 전송)
            // 여기서는 RecommendationEngine의 syncToAiServer 메서드 시그니처에 맞춰 호출
            // syncToAiServer(Long id, String type, String content, String status)
            
            // 리뷰 점수 등을 포함한 상세 내용은 별도 DTO나 JSON으로 변환하여 content에 담을 수 있음
            // 현재 RecommendationEngine 인터페이스에 맞춰 구현
            
            String content = String.format(
                "{\"description\":\"%s\",\"scores\":{\"communication\":%d,\"debugging\":%d,\"framework\":%d,\"language\":%d,\"schedule\":%d}}",
                data.reviewDescription(),
                data.communicationScore(),
                data.debuggingScore(),
                data.frameworkScore(),
                data.languageScore(),
                data.scheduleScore()
            );

            recommendationEngine.syncToAiServer(
                data.projectId(),
                "PROJECT_REVIEW", 
                content,
                "COMPLETED"
            );
            
            log.info("AI 서버 동기화 완료 - projectId: {}", data.projectId());
        } catch (Exception e) {
            log.error("AI 서버 동기화 실패 - projectId: {}", data.projectId(), e);
        }
    }
}
