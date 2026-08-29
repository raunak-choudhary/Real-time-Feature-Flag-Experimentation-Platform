package com.rex.statistics;

/**
 * Standard normal helpers.
 *
 * <p>Implemented here rather than pulled from a library so the arithmetic is inspectable. A
 * reviewer can check the approximation against a published table; they cannot check a dependency
 * they have to take on trust.
 */
final class NormalDistribution {

  private NormalDistribution() {}

  /** Cumulative distribution function. */
  static double cdf(double x) {
    return 0.5 * (1.0 + erf(x / Math.sqrt(2.0)));
  }

  /**
   * Error function, Abramowitz and Stegun 7.1.26.
   *
   * <p>Maximum absolute error 1.5e-7, comfortably below the four decimal places any experiment
   * report needs.
   */
  static double erf(double x) {
    double sign = Math.signum(x);
    double absoluteX = Math.abs(x);

    double t = 1.0 / (1.0 + 0.3275911 * absoluteX);
    double polynomial =
        t
            * (0.254829592
                + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429))));
    double result = 1.0 - polynomial * Math.exp(-absoluteX * absoluteX);

    return sign * result;
  }

  /**
   * Inverse CDF, used to turn a confidence level into a critical value.
   *
   * <p>Acklam's rational approximation, accurate to about 1.15e-9 across the range.
   */
  static double inverseCdf(double probability) {
    if (probability <= 0.0 || probability >= 1.0) {
      throw new IllegalArgumentException("probability must be strictly between 0 and 1");
    }

    double[] a = {
      -3.969683028665376e+01, 2.209460984245205e+02, -2.759285104469687e+02,
      1.383577518672690e+02, -3.066479806614716e+01, 2.506628277459239e+00
    };
    double[] b = {
      -5.447609879822406e+01,
      1.615858368580409e+02,
      -1.556989798598866e+02,
      6.680131188771972e+01,
      -1.328068155288572e+01
    };
    double[] c = {
      -7.784894002430293e-03, -3.223964580411365e-01, -2.400758277161838e+00,
      -2.549732539343734e+00, 4.374664141464968e+00, 2.938163982698783e+00
    };
    double[] d = {
      7.784695709041462e-03, 3.224671290700398e-01, 2.445134137142996e+00, 3.754408661907416e+00
    };

    double lowBreak = 0.02425;
    double highBreak = 1.0 - lowBreak;

    if (probability < lowBreak) {
      double q = Math.sqrt(-2.0 * Math.log(probability));
      return (((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
          / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
    }
    if (probability > highBreak) {
      double q = Math.sqrt(-2.0 * Math.log(1.0 - probability));
      return -(((((c[0] * q + c[1]) * q + c[2]) * q + c[3]) * q + c[4]) * q + c[5])
          / ((((d[0] * q + d[1]) * q + d[2]) * q + d[3]) * q + 1.0);
    }

    double q = probability - 0.5;
    double r = q * q;
    return (((((a[0] * r + a[1]) * r + a[2]) * r + a[3]) * r + a[4]) * r + a[5])
        * q
        / (((((b[0] * r + b[1]) * r + b[2]) * r + b[3]) * r + b[4]) * r + 1.0);
  }

  /** The two tailed critical value for a confidence level given as a percentage. */
  static double criticalValue(double confidenceLevelPercent) {
    double alpha = 1.0 - confidenceLevelPercent / 100.0;
    return inverseCdf(1.0 - alpha / 2.0);
  }
}
