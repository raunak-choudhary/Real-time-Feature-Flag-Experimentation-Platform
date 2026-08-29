package com.rex.api.dto;

import com.rex.model.FeatureFlag;
import java.time.LocalDateTime;

/** Feature flag as returned by the API. Never the entity, so persistence stays internal. */
public record FeatureFlagResponse(
    Long id,
    String name,
    String description,
    boolean enabled,
    FeatureFlag.FlagStatus status,
    int rolloutPercentage,
    String environment,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
