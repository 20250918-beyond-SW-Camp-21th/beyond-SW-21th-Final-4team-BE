package com.fallguys.recruitment.api.shared;

public interface ProjectCommandApi {
    void completeProjectWithAiSync(Long projectId, String reviewDescription, Object reviewScores);
}
