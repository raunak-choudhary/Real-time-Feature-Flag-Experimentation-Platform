package com.rex.statistics;

/**
 * How many users an experiment needs before its result means anything.
 *
 * <p>Standard two proportion sample size formula. Given a baseline rate, the smallest effect worth
 * detecting, a significance level and a desired power, it returns the required exposures per
 * variant.
 *
 * <p>Power defaults to 80 percent, the conventional choice. Power is the probability of detecting a
 * real effect when one exists, so at 80 percent a genuine improvement is missed one time in five.
 * That is a deliberate trade against sample size rather than an oversight.
 */
public final class SampleSizeCalculator {

  /** Conventional default: an 80 percent chance of detecting a real effect. */
  public static final double DEFAULT_POWER = 80.0;

  private SampleSizeCalculator() {}

  /**
   * Required exposures per variant.
   *
   * @param baselineRate the control conversion rate, between 0 and 1
   * @param minimumDetectableEffect the smallest absolute change worth detecting, for example 0.02
   *     for two percentage points
   */
  public static long requiredPerVariant(
      double baselineRate,
      double minimumDetectableEffect,
      double confidenceLevelPercent,
      double powerPercent) {

    if (baselineRate <= 0.0 || baselineRate >= 1.0) {
      throw new IllegalArgumentException("baselineRate must be strictly between 0 and 1");
    }
    if (minimumDetectableEffect <= 0.0) {
      throw new IllegalArgumentException("minimumDetectableEffect must be positive");
    }

    double treatmentRate = Math.min(0.999999, baselineRate + minimumDetectableEffect);

    double zAlpha = NormalDistribution.criticalValue(confidenceLevelPercent);
    double zBeta = NormalDistribution.inverseCdf(powerPercent / 100.0);

    double pooled = (baselineRate + treatmentRate) / 2.0;
    double numerator =
        Math.pow(
            zAlpha * Math.sqrt(2.0 * pooled * (1.0 - pooled))
                + zBeta
                    * Math.sqrt(
                        baselineRate * (1.0 - baselineRate)
                            + treatmentRate * (1.0 - treatmentRate)),
            2.0);
    double denominator = Math.pow(treatmentRate - baselineRate, 2.0);

    return (long) Math.ceil(numerator / denominator);
  }

  /** Convenience overload using the conventional 80 percent power. */
  public static long requiredPerVariant(
      double baselineRate, double minimumDetectableEffect, double confidenceLevelPercent) {
    return requiredPerVariant(
        baselineRate, minimumDetectableEffect, confidenceLevelPercent, DEFAULT_POWER);
  }
}
