package com.rex.rollout;

import com.rex.model.Metrics;
import com.rex.repository.MetricsRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Measures guardrail metrics against the cohort that was actually exposed.
 *
 * <p>Filtering on the served decision is what makes a breach attributable. Comparing the whole
 * population against a threshold would count users who never saw the change, so a pre existing
 * background error rate would look like the rollout causing harm.
 */
@Component
public class GuardrailEvaluator {

  private static final Logger logger = LoggerFactory.getLogger(GuardrailEvaluator.class);

  /** Fallback window when a stage has no recorded entry time. */
  private static final Duration FALLBACK_WINDOW = Duration.ofMinutes(30);

  private final MetricsRepository metricsRepository;

  public GuardrailEvaluator(MetricsRepository metricsRepository) {
    this.metricsRepository = metricsRepository;
  }

  /**
   * Measures each guardrail over the life of the current stage.
   *
   * <p>Measuring from when the stage was entered rather than over a fixed trailing window is what
   * attributes a breach to this stage. A trailing window drags in behaviour from an earlier,
   * smaller percentage and dilutes exactly the signal the guardrail exists to catch.
   */
  public List<GuardrailVerdict> evaluate(
      Long flagId, List<Guardrail> guardrails, LocalDateTime stageEnteredAt, LocalDateTime now) {

    LocalDateTime since = stageEnteredAt != null ? stageEnteredAt : now.minus(FALLBACK_WINDOW);
    long exposures = metricsRepository.countExposuresByDecisionSince(flagId, true, since);

    return guardrails.stream().map(g -> evaluateOne(flagId, g, exposures, since)).toList();
  }

  private GuardrailVerdict evaluateOne(
      Long flagId, Guardrail guardrail, long exposures, LocalDateTime since) {

    if (exposures < guardrail.minimumObservations()) {
      return new GuardrailVerdict(
          guardrail, 0.0, exposures, GuardrailVerdict.Status.INSUFFICIENT_DATA);
    }

    Double observed;
    try {
      observed = measure(flagId, guardrail.metric(), exposures, since);
    } catch (RuntimeException exception) {
      logger.error(
          "Guardrail {} could not be measured for flag {}", guardrail.metric(), flagId, exception);
      return new GuardrailVerdict(guardrail, 0.0, exposures, GuardrailVerdict.Status.UNAVAILABLE);
    }

    if (observed == null) {
      return new GuardrailVerdict(guardrail, 0.0, exposures, GuardrailVerdict.Status.UNAVAILABLE);
    }

    return new GuardrailVerdict(
        guardrail,
        observed,
        exposures,
        guardrail.breachedBy(observed)
            ? GuardrailVerdict.Status.BREACHED
            : GuardrailVerdict.Status.HEALTHY);
  }

  private Double measure(
      Long flagId, Guardrail.GuardrailMetric metric, long exposures, LocalDateTime since) {
    return switch (metric) {
      case ERROR_RATE ->
          (double) metricsRepository.countEventsByTypeSince(flagId, Metrics.EventType.ERROR, since)
              / exposures;
      case CONVERSION_RATE ->
          (double)
                  metricsRepository.countEventsByTypeSince(
                      flagId, Metrics.EventType.CONVERSION, since)
              / exposures;
      case AVERAGE_LOAD_TIME_MS ->
          metricsRepository.averageDurationByTypeSince(flagId, Metrics.EventType.LOAD_TIME, since);
    };
  }
}
