package com.fallguys.recruitment.api.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiRecommendationResponseDTO(
        Long id,
        @JsonAlias({"title", "name"})
        String nameOrTitle,
        Double matchScore,
        List<String> skills,
        String description,
        Long budget,
        Integer duration
) {
    public AiRecommendationResponseDTO {
        skills = skills == null ? java.util.List.of() : java.util.List.copyOf(skills);
    }

    public AiRecommendationResponseDTO withFreelancerInfo(String nameOrTitle, List<String> skills, String description) {
        return new AiRecommendationResponseDTO(id, nameOrTitle, matchScore, skills, description, budget, duration);
    }

    public AiRecommendationResponseDTO withJobInfo(List<String> skills, String description, Long budget, Integer duration) {
        return new AiRecommendationResponseDTO(id, nameOrTitle, matchScore, skills, description, budget, duration);
    }
}
