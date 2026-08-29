package com.rex.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Payload for changing only a flag's rollout percentage. */
public record RolloutUpdateRequest(
    @NotNull(message = "rolloutPercentage is required")
        @Min(value = 0, message = "rolloutPercentage must be at least 0")
        @Max(value = 100, message = "rolloutPercentage must be at most 100")
        Integer rolloutPercentage) {}
