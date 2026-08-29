package com.rex.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating or replacing an experiment.
 *
 * <p>Traffic percentage starts at 1 rather than 0: an experiment allocated no traffic can never
 * enrol anyone, so accepting it would create a record that looks configured but can never produce a
 * result.
 */
public record ExperimentRequest(
    @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must be at most 100 characters")
        String name,
    @Size(max = 1000, message = "description must be at most 1000 characters") String description,
    @Size(max = 2000, message = "hypothesis must be at most 2000 characters") String hypothesis,
    @Size(max = 255, message = "successMetric must be at most 255 characters") String successMetric,
    @Min(value = 1, message = "trafficPercentage must be at least 1")
        @Max(value = 100, message = "trafficPercentage must be at most 100")
        Integer trafficPercentage,
    @Size(max = 255, message = "controlVariantName must be at most 255 characters")
        String controlVariantName,
    @Size(max = 255, message = "testVariantName must be at most 255 characters")
        String testVariantName,
    @DecimalMin(value = "80.0", message = "confidenceLevel must be at least 80")
        @DecimalMax(value = "99.9", message = "confidenceLevel must be at most 99.9")
        Double confidenceLevel,
    @Min(value = 1, message = "minimumSampleSize must be at least 1") Integer minimumSampleSize,
    Double expectedImprovement,
    @Size(max = 50, message = "environment must be at most 50 characters") String environment,
    @Size(max = 100, message = "createdBy must be at most 100 characters") String createdBy) {}
