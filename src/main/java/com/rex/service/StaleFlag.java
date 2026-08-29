package com.rex.service;

import java.time.LocalDateTime;

/** A flag with no recent evaluation. Domain type, so the service layer stays free of the API. */
public record StaleFlag(
    Long id,
    String name,
    String environment,
    boolean enabled,
    int rolloutPercentage,
    LocalDateTime lastEvaluatedAt,
    long daysSinceLastEvaluation) {}
