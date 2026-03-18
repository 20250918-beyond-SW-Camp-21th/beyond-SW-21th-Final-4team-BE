package com.fallguys.common.ai.port;

import java.util.List;

public interface RecommendationEngine {
    <T> List<T> recommend(String type, Long id, Class<T> responseType);

    <T> List<T> recommendFreelancers(Long jobId, String title, String description, String skills, Class<T> responseType);

    <T> List<T> recommendJobs(Long freelancerId, String skills, String experience, Class<T> responseType);

    void syncToAiServer(Long id, Long refId, String type, String content, String status);

    void syncProjectExperience(Long documentId, Long refId, String content, String status);
}
