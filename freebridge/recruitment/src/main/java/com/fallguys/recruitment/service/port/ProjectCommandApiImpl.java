package com.fallguys.recruitment.service.port;

import com.fallguys.common.ai.port.RecommendationEngine;
import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.recruitment.api.shared.ProjectCommandApi;
import com.fallguys.recruitment.entity.Project;
import com.fallguys.recruitment.repository.ProjectPostingRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectCommandApiImpl implements ProjectCommandApi {
    private final ProjectPostingRepo projectPostingRepo;
    private final RecommendationEngine recommendationEngine;

    @Override
    @Transactional
    public void completeProjectWithAiSync(Long projectId, String reviewDescription, Object reviewScores) {
        Project project = projectPostingRepo.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
        project.complete();


        recommendationEngine.syncToAiServer(
                project.getFreelancerId(),
                "experience",
                reviewDescription,
                "COMPLETED"
        );
    }
}