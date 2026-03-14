package com.fallguys.recruitment.service.port;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.recruitment.api.shared.ProjectCommandApi;
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import com.fallguys.recruitment.service.ReviewSyncPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectCommandApiImpl implements ProjectCommandApi {
    private final ProjectPostingRepo projectPostingRepo;
    private final RecommendationEngine recommendationEngine;
    private final ObjectMapper objectMapper;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @Override
    @Transactional
    public void completeProjectWithAiSync(Long projectId, String reviewDescription, Object reviewScores) {
        Project project = projectPostingRepo.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        project.complete();

        Runnable syncTask = () -> {
            try {
                String content = objectMapper.writeValueAsString(new ReviewSyncPayload(reviewDescription, reviewScores));
                recommendationEngine.syncProjectExperience(
                        projectId,
                        project.getFreelancerId(),
                        content,
                        "COMPLETED"
                );
            } catch (Exception e) {
                log.error("프로젝트 리뷰 AI 동기화 실패 - projectId={}, freelancerId={}", projectId, project.getFreelancerId(), e);
            }
        };

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
}
