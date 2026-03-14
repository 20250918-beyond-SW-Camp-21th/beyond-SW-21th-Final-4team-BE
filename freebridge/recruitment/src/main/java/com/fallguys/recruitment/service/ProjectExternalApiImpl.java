package com.fallguys.recruitment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.common.port.ProjectExternalApi;
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class ProjectExternalApiImpl implements ProjectExternalApi {

    private final ProjectPostingRepo projectPostingRepo;
    private final RecommendationEngine recommendationEngine;
    private final Executor taskExecutor;
    private final ObjectMapper objectMapper;

    public ProjectExternalApiImpl(
            ProjectPostingRepo projectPostingRepo,
            RecommendationEngine recommendationEngine,
            @Qualifier("taskExecutor") Executor taskExecutor,
            ObjectMapper objectMapper) {
        this.projectPostingRepo = projectPostingRepo;
        this.recommendationEngine = recommendationEngine;
        this.taskExecutor = taskExecutor;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void completeProjectWithReview(ProjectCompletionData data) {
        Project project = projectPostingRepo.findById(data.projectId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

        project.complete();

        Long freelancerId = project.getFreelancerId();
        if (!java.util.Objects.equals(freelancerId, data.freelancerId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Runnable syncTask = () -> syncToAiServer(data, freelancerId);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(syncTask, taskExecutor);
                }
            });
        } else {
            CompletableFuture.runAsync(syncTask, taskExecutor);
        }
    }

    private void syncToAiServer(ProjectCompletionData data, Long freelancerId) {
        try {
            ReviewSyncPayload payload = new ReviewSyncPayload(
                    data.reviewDescription(),
                    Map.of(
                            "communication", data.communicationScore(),
                            "debugging", data.debuggingScore(),
                            "framework", data.frameworkScore(),
                            "language", data.languageScore(),
                            "schedule", data.scheduleScore()
                    )
            );
            String content = objectMapper.writeValueAsString(payload);

            recommendationEngine.syncProjectExperience(
                    data.projectId(),
                    freelancerId,
                    content,
                    "COMPLETED"
            );

            log.info("AI 서버 동기화 완료 - projectId: {}", data.projectId());
        } catch (Exception e) {
            log.error("AI 서버 동기화 실패 - projectId: {}", data.projectId(), e);
        }
    }
}
