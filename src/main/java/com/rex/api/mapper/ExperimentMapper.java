package com.rex.api.mapper;

import com.rex.api.dto.AssignmentResponse;
import com.rex.api.dto.ExperimentRequest;
import com.rex.api.dto.ExperimentResponse;
import com.rex.model.Experiment;
import com.rex.model.UserCohort;
import org.springframework.stereotype.Component;

/** Translates between the experiment and cohort entities and their API representations. */
@Component
public class ExperimentMapper {

  private static final String DEFAULT_ENVIRONMENT = "development";
  private static final int DEFAULT_TRAFFIC_PERCENTAGE = 50;
  private static final double DEFAULT_CONFIDENCE_LEVEL = 95.0;

  public Experiment toEntity(ExperimentRequest request) {
    Experiment experiment = new Experiment();
    applyTo(experiment, request);
    return experiment;
  }

  /** Copies request values onto an existing experiment, defaulting the optional fields. */
  public void applyTo(Experiment experiment, ExperimentRequest request) {
    experiment.setName(request.name());
    experiment.setDescription(request.description());
    experiment.setHypothesis(request.hypothesis());
    experiment.setSuccessMetric(request.successMetric());
    experiment.setTrafficPercentage(
        request.trafficPercentage() != null
            ? request.trafficPercentage()
            : DEFAULT_TRAFFIC_PERCENTAGE);
    if (request.controlVariantName() != null) {
      experiment.setControlVariantName(request.controlVariantName());
    }
    if (request.testVariantName() != null) {
      experiment.setTestVariantName(request.testVariantName());
    }
    experiment.setConfidenceLevel(
        request.confidenceLevel() != null ? request.confidenceLevel() : DEFAULT_CONFIDENCE_LEVEL);
    experiment.setMinimumSampleSize(request.minimumSampleSize());
    experiment.setExpectedImprovement(request.expectedImprovement());
    experiment.setEnvironment(
        request.environment() != null ? request.environment() : DEFAULT_ENVIRONMENT);
    if (request.createdBy() != null) {
      experiment.setCreatedBy(request.createdBy());
    }
  }

  public ExperimentResponse toResponse(Experiment experiment) {
    return new ExperimentResponse(
        experiment.getId(),
        experiment.getName(),
        experiment.getDescription(),
        experiment.getStatus(),
        experiment.getHypothesis(),
        experiment.getSuccessMetric(),
        experiment.getTrafficPercentage() != null ? experiment.getTrafficPercentage() : 0,
        experiment.getControlVariantName(),
        experiment.getTestVariantName(),
        experiment.getConfidenceLevel(),
        experiment.getMinimumSampleSize(),
        experiment.getCurrentSampleSize(),
        experiment.getExpectedImprovement(),
        experiment.getEnvironment(),
        experiment.getCreatedBy(),
        experiment.getStartDate(),
        experiment.getEndDate(),
        experiment.getCreatedAt(),
        experiment.getUpdatedAt());
  }

  public AssignmentResponse toAssignmentResponse(UserCohort cohort) {
    Experiment experiment = cohort.getExperiment();
    return new AssignmentResponse(
        cohort.getUserId(),
        experiment != null ? experiment.getId() : null,
        experiment != null ? experiment.getName() : null,
        cohort.getVariantName(),
        cohort.getCohortType() != null ? cohort.getCohortType().name() : null,
        cohort.getAssignmentHash(),
        cohort.getAssignedAt());
  }
}
