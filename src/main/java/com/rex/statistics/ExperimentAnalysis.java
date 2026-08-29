package com.rex.statistics;

/**
 * The complete picture for one experiment: the comparison, and whether it may be believed yet.
 *
 * <p>The two travel together deliberately. A significance verdict without a readiness check is what
 * lets an under powered experiment be reported as a win.
 */
public record ExperimentAnalysis(SignificanceResult significance, ExperimentReadiness readiness) {

  /**
   * Whether a winner may be declared.
   *
   * <p>Requires both a significant difference and a sufficient sample. A p-value below the
   * threshold on half the required data is exactly the result the peeking guard exists to refuse.
   */
  public boolean canDeclareWinner() {
    return readiness.ready() && significance.significant();
  }

  /** A short human readable summary, so a dashboard does not have to assemble the sentence. */
  public String summary() {
    if (!readiness.ready()) {
      return "Inconclusive: %d more per variant needed".formatted(readiness.remaining());
    }
    return switch (significance.verdict()) {
      case TEST_WINS -> "Test wins, p = %.4f".formatted(significance.pValue());
      case CONTROL_WINS -> "Control wins, p = %.4f".formatted(significance.pValue());
      case NO_SIGNIFICANT_DIFFERENCE ->
          "No significant difference, p = %.4f".formatted(significance.pValue());
      case INSUFFICIENT_DATA -> "Insufficient data";
    };
  }
}
