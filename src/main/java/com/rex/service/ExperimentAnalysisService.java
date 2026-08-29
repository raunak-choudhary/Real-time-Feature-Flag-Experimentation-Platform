package com.rex.service;

import com.rex.model.Experiment;
import com.rex.model.Metrics;
import com.rex.repository.MetricsRepository;
import com.rex.statistics.ConversionAnalyzer;
import com.rex.statistics.ExperimentAnalysis;
import com.rex.statistics.ExperimentReadiness;
import com.rex.statistics.SampleSizeCalculator;
import com.rex.statistics.SignificanceResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the numbers an experiment result needs and hands them to the statistics engine.
 *
 * <p>This class does the counting and the plumbing; it deliberately does no arithmetic of its own,
 * so the maths stays in one pure, independently tested place.
 */
@Service
@Transactional(readOnly = true)
public class ExperimentAnalysisService {

  /** Used when an experiment has not declared its own minimum detectable effect. */
  private static final double DEFAULT_MINIMUM_DETECTABLE_EFFECT = 0.02;

  private final MetricsRepository metricsRepository;

  public ExperimentAnalysisService(MetricsRepository metricsRepository) {
    this.metricsRepository = metricsRepository;
  }

  public ExperimentAnalysis analyse(Experiment experiment) {
    String control = experiment.getControlVariantName();
    String test = experiment.getTestVariantName();

    long controlExposures = exposuresFor(experiment.getId(), control);
    long testExposures = exposuresFor(experiment.getId(), test);
    long controlConversions = conversionsFor(experiment.getId(), control);
    long testConversions = conversionsFor(experiment.getId(), test);

    double confidence =
        experiment.getConfidenceLevel() != null ? experiment.getConfidenceLevel() : 95.0;

    SignificanceResult significance =
        ConversionAnalyzer.compare(
            controlConversions, controlExposures, testConversions, testExposures, confidence);

    long required = requiredSampleFor(experiment, significance.controlRate(), confidence);
    long current = Math.min(controlExposures, testExposures);
    ExperimentReadiness readiness = ExperimentReadiness.evaluate(current, required);
    return new ExperimentAnalysis(significance, readiness);
  }

  private long exposuresFor(Long experimentId, String variant) {
    return metricsRepository.countDistinctUsersByExperimentAndVariant(experimentId, variant);
  }

  private long conversionsFor(Long experimentId, String variant) {
    return metricsRepository.countByExperimentVariantAndType(
        experimentId, variant, Metrics.EventType.CONVERSION);
  }

  /**
   * The sample the experiment planned for.
   *
   * <p>An explicit minimum is honoured. Otherwise it is derived from the observed baseline, so an
   * experiment that never declared one still gets a real threshold rather than none at all.
   */
  private long requiredSampleFor(Experiment experiment, double baselineRate, double confidence) {
    if (experiment.getMinimumSampleSize() != null && experiment.getMinimumSampleSize() > 0) {
      return experiment.getMinimumSampleSize();
    }
    if (baselineRate <= 0.0 || baselineRate >= 1.0) {
      // No usable baseline yet, so nothing can be concluded regardless of the numbers.
      return Long.MAX_VALUE;
    }
    double effect =
        experiment.getExpectedImprovement() != null && experiment.getExpectedImprovement() > 0
            ? experiment.getExpectedImprovement() / 100.0
            : DEFAULT_MINIMUM_DETECTABLE_EFFECT;
    return SampleSizeCalculator.requiredPerVariant(baselineRate, effect, confidence);
  }
}
