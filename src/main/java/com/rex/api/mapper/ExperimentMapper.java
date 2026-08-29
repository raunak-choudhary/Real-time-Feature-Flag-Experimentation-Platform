package com.rex.api.mapper;

import com.rex.api.dto.AssignmentResponse;
import com.rex.api.dto.ExperimentAnalysisResponse;
import com.rex.api.dto.ExperimentResponse;
import com.rex.model.Experiment;
import com.rex.model.UserCohort;
import com.rex.statistics.ConfidenceInterval;
import com.rex.statistics.ExperimentAnalysis;
import com.rex.statistics.ExperimentReadiness;
import com.rex.statistics.SignificanceResult;
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

  /** Maps the statistical outcome onto the API shape. */
  public ExperimentAnalysisResponse toAnalysisResponse(
      Experiment experiment, ExperimentAnalysis analysis) {

    SignificanceResult significance = analysis.significance();
    ExperimentReadiness readiness = analysis.readiness();
    ConfidenceInterval controlInterval = significance.controlInterval();
    ConfidenceInterval testInterval = significance.testInterval();

    return new ExperimentAnalysisResponse(
        experiment.getId(),
        experiment.getName(),
        experiment.getControlVariantName(),
        experiment.getTestVariantName(),
        significance.controlExposures(),
        significance.controlConversions(),
        significance.controlRate(),
        controlInterval != null ? controlInterval.lower() : null,
        controlInterval != null ? controlInterval.upper() : null,
        significance.testExposures(),
        significance.testConversions(),
        significance.testRate(),
        testInterval != null ? testInterval.lower() : null,
        testInterval != null ? testInterval.upper() : null,
        significance.absoluteLift(),
        significance.relativeLift(),
        significance.zScore(),
        significance.pValue(),
        significance.significant(),
        significance.confidenceLevel(),
        readiness.currentPerVariant(),
        readiness.requiredPerVariant(),
        readiness.remaining(),
        readiness.progress(),
        readiness.ready(),
        analysis.canDeclareWinner(),
        significance.verdict().name(),
        analysis.summary());
  }
}
