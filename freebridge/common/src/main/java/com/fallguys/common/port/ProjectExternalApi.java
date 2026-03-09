package com.fallguys.common.port;

public interface ProjectExternalApi {

    void completeProjectWithReview(ProjectCompletionData data);

    record ProjectCompletionData(
        Long projectId,
        Long freelancerId,
        String reviewDescription,
        int communicationScore,
        int debuggingScore,
        int frameworkScore,
        int languageScore,
        int scheduleScore
    ) {}
}
