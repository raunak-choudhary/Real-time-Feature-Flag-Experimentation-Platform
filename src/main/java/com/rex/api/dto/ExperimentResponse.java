package com.rex.api.dto;

import com.rex.model.Experiment;
import java.time.LocalDateTime;

/** Experiment as returned by the API. */
public record ExperimentResponse(
    Long id,
    String name,
    String description,
    Experiment.ExperimentStatus status,
    String hypothesis,
    String successMetric,
    int trafficPercentage,
    String controlVariantName,
    String testVariantName,
    Double confidenceLevel,
    Integer minimumSampleSize,
    Integer currentSampleSize,
    Double expectedImprovement,
    String environment,
    String createdBy,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
