package com.rex.statistics;

/**
 * The outcome of comparing two conversion rates.
 *
 * <p>Carries the interval and the sample counts alongside the verdict, because a p-value on its own
 * tells a reader whether to believe the result but nothing about how large it is.
 */
public record SignificanceResult(
    long controlConversions,
    long controlExposures,
    long testConversions,
    long testExposures,
    double controlRate,
    double testRate,
    double absoluteLift,
    double relativeLift,
    double zScore,
    double pValue,
    boolean significant,
    double confidenceLevel,
    ConfidenceInterval controlInterval,
    ConfidenceInterval testInterval,
    Verdict verdict) {

  /** What the numbers permit us to say. */
  public enum Verdict {
    /** Not enough data to conclude anything, regardless of the p-value. */
    INSUFFICIENT_DATA,
    /** Enough data, but the difference is not distinguishable from noise. */
    NO_SIGNIFICANT_DIFFERENCE,
    /** The test variant is significantly better. */
    TEST_WINS,
    /** The control variant is significantly better. */
    CONTROL_WINS
  }
}
