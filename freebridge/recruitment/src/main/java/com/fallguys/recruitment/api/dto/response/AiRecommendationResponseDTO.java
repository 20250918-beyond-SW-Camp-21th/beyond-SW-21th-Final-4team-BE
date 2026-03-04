package com.fallguys.recruitment.api.dto.response;

public record AiRecommendationResponseDTO(
        Long id,
        String nameOrTitle,
        Double matchScore
) {
}
