package com.fallguys.matchs.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ApplicationCreateRequest(
        @NotNull
        @Positive
        Long jobPostingId,

        @NotBlank
        @Size(max = 1000)
        String message
) {
}
