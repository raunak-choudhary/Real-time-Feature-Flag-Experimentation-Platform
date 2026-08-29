package com.rex.api.mapper;

import com.rex.api.dto.AssignmentResponse;
import com.rex.api.dto.ExperimentResponse;
import com.rex.model.Experiment;
import com.rex.model.UserCohort;
import org.springframework.stereotype.Component;

/** Translates between the experiment and cohort entities and their API representations. */
@Component
public class ExperimentMapper {

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
