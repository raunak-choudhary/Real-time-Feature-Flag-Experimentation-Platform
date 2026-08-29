package com.rex.api.dto;

import com.rex.model.FeatureFlag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or replacing a feature flag.
 *
 * <p>Rollout bounds are enforced here rather than in the service so an out-of-range value is
 * rejected at the edge with a field-level message, instead of surfacing later as a constraint
 * violation from the database.
 */
public record FeatureFlagRequest(
    @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        @Pattern(
            regexp = "^[a-z0-9_]+$",
            message = "name must be lower case alphanumeric with underscores")
        String name,
    @Size(max = 500, message = "description must be at most 500 characters") String description,
    Boolean enabled,
    FeatureFlag.FlagStatus status,
    @Min(value = 0, message = "rolloutPercentage must be at least 0")
        @Max(value = 100, message = "rolloutPercentage must be at most 100")
        Integer rolloutPercentage,
    @Size(max = 50, message = "environment must be at most 50 characters") String environment,
    @Size(max = 100, message = "createdBy must be at most 100 characters") String createdBy) {}
