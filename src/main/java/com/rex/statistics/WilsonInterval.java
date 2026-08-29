package com.rex.statistics;

/**
 * Wilson score interval for a binomial proportion.
 *
 * <p>Chosen over the normal approximation because the naive interval misbehaves exactly where
 * experiments live. At low conversion counts it produces a lower bound below zero, which is not a
 * possible conversion rate, and at zero conversions it collapses to a zero width interval implying
 * certainty from no evidence at all. Wilson stays inside 0 to 1 and keeps a sensible width when
 * counts are small.
 */
public final class WilsonInterval {

  private WilsonInterval() {}

  public static ConfidenceInterval forProportion(
      long successes, long trials, double confidenceLevelPercent) {

    if (trials <= 0) {
      throw new IllegalArgumentException("trials must be positive");
    }
    if (successes < 0 || successes > trials) {
      throw new IllegalArgumentException("successes must be between 0 and trials");
    }

    double z = NormalDistribution.criticalValue(confidenceLevelPercent);
    double n = trials;
    double proportion = (double) successes / n;
    double zSquared = z * z;

    double denominator = 1.0 + zSquared / n;
    double centre = (proportion + zSquared / (2.0 * n)) / denominator;
    double margin =
        (z / denominator)
            * Math.sqrt(proportion * (1.0 - proportion) / n + zSquared / (4.0 * n * n));

    return new ConfidenceInterval(
        Math.max(0.0, centre - margin), Math.min(1.0, centre + margin), confidenceLevelPercent);
  }
}
