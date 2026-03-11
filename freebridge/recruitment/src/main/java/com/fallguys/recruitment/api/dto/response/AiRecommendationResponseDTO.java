package com.fallguys.recruitment.api.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;

public record AiRecommendationResponseDTO(
        Long id,
        @JsonAlias({"title", "name"})
        String nameOrTitle,
        Double matchScore
) {
}
