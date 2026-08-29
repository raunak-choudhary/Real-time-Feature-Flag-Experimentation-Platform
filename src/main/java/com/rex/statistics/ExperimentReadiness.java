package com.rex.statistics;

/**
 * Whether an experiment has earned the right to declare a result.
 *
 * <p>This is the peeking guard. Checking a running experiment repeatedly and stopping the moment
 * the p-value dips below the threshold inflates the false positive rate far past the nominal five
 * percent, because a p-value that wanders will eventually wander below any line. Gating on a
 * predetermined sample size is what makes the significance test mean what it claims.
 */
public record ExperimentReadiness(
    long currentPerVariant, long requiredPerVariant, boolean ready, long remaining) {

  public static ExperimentReadiness evaluate(long currentPerVariant, long requiredPerVariant) {
    boolean ready = currentPerVariant >= requiredPerVariant;
    return new ExperimentReadiness(
        currentPerVariant,
        requiredPerVariant,
        ready,
        ready ? 0 : requiredPerVariant - currentPerVariant);
  }

  /** Progress toward the required sample, for a dashboard to show rather than a bare boolean. */
  public double progress() {
    if (requiredPerVariant <= 0) {
      return 1.0;
    }
    return Math.min(1.0, (double) currentPerVariant / requiredPerVariant);
  }
}
