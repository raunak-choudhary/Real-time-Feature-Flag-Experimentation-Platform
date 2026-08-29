package com.rex.statistics;

/**
 * Compares two conversion rates and says whether the difference is real.
 *
 * <p>Two proportion z-test with a pooled standard error, which is the standard fixed horizon test
 * for this shape of data. Pure by design: no Spring, no repository, no clock, so the arithmetic can
 * be checked against published worked examples rather than against itself.
 *
 * <p>Deliberately frequentist and fixed horizon. Peeking at a running experiment and stopping when
 * the p-value dips below the threshold inflates the false positive rate well beyond the nominal
 * five percent, which is why the sample size gate exists alongside this class rather than as an
 * optional extra.
 */
public final class ConversionAnalyzer {

  /** Below this many exposures per variant the test is not worth running at all. */
  private static final long MINIMUM_EXPOSURES_PER_VARIANT = 30;

  private ConversionAnalyzer() {}

  public static SignificanceResult compare(
      long controlConversions,
      long controlExposures,
      long testConversions,
      long testExposures,
      double confidenceLevelPercent) {

    validate(controlConversions, controlExposures, "control");
    validate(testConversions, testExposures, "test");

    if (controlExposures < MINIMUM_EXPOSURES_PER_VARIANT
        || testExposures < MINIMUM_EXPOSURES_PER_VARIANT) {
      return insufficient(
          controlConversions,
          controlExposures,
          testConversions,
          testExposures,
          confidenceLevelPercent);
    }

    double controlRate = (double) controlConversions / controlExposures;
    double testRate = (double) testConversions / testExposures;

    double pooledRate =
        (double) (controlConversions + testConversions) / (controlExposures + testExposures);
    double standardError =
        Math.sqrt(pooledRate * (1.0 - pooledRate) * (1.0 / controlExposures + 1.0 / testExposures));

    // Identical rates, or no conversions at all, leave nothing to distinguish.
    double zScore = standardError == 0.0 ? 0.0 : (testRate - controlRate) / standardError;
    double pValue = twoTailedPValue(zScore);

    double alpha = 1.0 - confidenceLevelPercent / 100.0;
    boolean significant = pValue < alpha;

    SignificanceResult.Verdict verdict;
    if (!significant) {
      verdict = SignificanceResult.Verdict.NO_SIGNIFICANT_DIFFERENCE;
    } else if (testRate > controlRate) {
      verdict = SignificanceResult.Verdict.TEST_WINS;
    } else {
      verdict = SignificanceResult.Verdict.CONTROL_WINS;
    }

    return new SignificanceResult(
        controlConversions,
        controlExposures,
        testConversions,
        testExposures,
        controlRate,
        testRate,
        testRate - controlRate,
        controlRate == 0.0 ? 0.0 : (testRate - controlRate) / controlRate,
        zScore,
        pValue,
        significant,
        confidenceLevelPercent,
        WilsonInterval.forProportion(controlConversions, controlExposures, confidenceLevelPercent),
        WilsonInterval.forProportion(testConversions, testExposures, confidenceLevelPercent),
        verdict);
  }

  /** Two tailed p-value for a z statistic. */
  static double twoTailedPValue(double zScore) {
    return 2.0 * (1.0 - NormalDistribution.cdf(Math.abs(zScore)));
  }

  private static SignificanceResult insufficient(
      long controlConversions,
      long controlExposures,
      long testConversions,
      long testExposures,
      double confidenceLevelPercent) {

    return new SignificanceResult(
        controlConversions,
        controlExposures,
        testConversions,
        testExposures,
        controlExposures == 0 ? 0.0 : (double) controlConversions / controlExposures,
        testExposures == 0 ? 0.0 : (double) testConversions / testExposures,
        0.0,
        0.0,
        0.0,
        1.0,
        false,
        confidenceLevelPercent,
        null,
        null,
        SignificanceResult.Verdict.INSUFFICIENT_DATA);
  }

  private static void validate(long conversions, long exposures, String variant) {
    if (exposures < 0) {
      throw new IllegalArgumentException(variant + " exposures cannot be negative");
    }
    if (conversions < 0) {
      throw new IllegalArgumentException(variant + " conversions cannot be negative");
    }
    if (conversions > exposures) {
      throw new IllegalArgumentException(variant + " conversions cannot exceed exposures");
    }
  }
}
