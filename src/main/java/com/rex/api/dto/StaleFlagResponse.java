package com.rex.api.dto;

import java.time.LocalDateTime;

/** A flag nobody has evaluated recently, and is therefore a candidate for removal. */
public record StaleFlagResponse(
    Long id,
    String name,
    String environment,
    boolean enabled,
    int rolloutPercentage,
    LocalDateTime lastEvaluatedAt,
    long daysSinceLastEvaluation) {}
