package com.fallguys.common.ai.dto;

import java.util.List;

public record FreelancerAiReputationReportDto(
        String grade,
        int positivityScore,
        String summary,
        List<String> strengths,
        List<String> weaknesses,
        List<ScoreDto> technicalScores,
        List<ScoreDto> softSkills
) {
    public record ScoreDto(String name, int score) {}
}