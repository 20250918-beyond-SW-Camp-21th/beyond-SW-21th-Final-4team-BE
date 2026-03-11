package com.fallguys.review.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmployerRejectionReasonCreateRequest(
        @NotNull
        Long projectId,

        @NotBlank
        @Size(max = 255)
        String projectTitle,

        @NotNull
        Long freelancerId,

        @NotBlank
        @Size(max = 1000)
        String reason
) {
}
