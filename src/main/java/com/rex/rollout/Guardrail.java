package com.rex.rollout;

/**
 * A metric threshold that halts a rollout when breached.
 *
 * <p>The minimum observation count is not optional decoration. Without it a single early error in
 * the first minutes of a rollout produces a 100 percent error rate and triggers an immediate
 * rollback, which is the same insufficient sample problem the experiment analysis already guards
 * against.
 */
public record Guardrail(
    GuardrailMetric metric, double threshold, Comparison comparison, long minimumObservations) {

  public static final long DEFAULT_MINIMUM_OBSERVATIONS = 100;

  public Guardrail(GuardrailMetric metric, double threshold, Comparison comparison) {
    this(metric, threshold, comparison, DEFAULT_MINIMUM_OBSERVATIONS);
  }

  /** Whether an observed value breaches this guardrail. */
  public boolean breachedBy(double observed) {
    return comparison == Comparison.ABOVE ? observed > threshold : observed < threshold;
  }

  /** What is being watched. Each maps to events the platform already records. */
  public enum GuardrailMetric {
    /** Proportion of exposures that produced an error event. */
    ERROR_RATE,
    /** Mean recorded load time in milliseconds. */
    AVERAGE_LOAD_TIME_MS,
    /** Proportion of exposures that converted, watched for a drop rather than a rise. */
    CONVERSION_RATE
  }

  /** Which direction constitutes a breach. */
  public enum Comparison {
    ABOVE,
    BELOW
  }
}
