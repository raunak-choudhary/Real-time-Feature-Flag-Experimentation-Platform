package com.rex.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The peeking guard, which is what makes the significance test mean what it claims. */
class ExperimentReadinessTest {

  @Test
  @DisplayName("an experiment below its threshold is not ready and reports what is missing")
  void belowThresholdIsNotReady() {
    ExperimentReadiness readiness = ExperimentReadiness.evaluate(3_000, 6_500);

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.remaining()).isEqualTo(3_500);
    assertThat(readiness.progress()).isCloseTo(0.4615, Offset.offset(0.001));
  }

  @Test
  @DisplayName("an experiment at or past its threshold is ready")
  void atThresholdIsReady() {
    assertThat(ExperimentReadiness.evaluate(6_500, 6_500).ready()).isTrue();
    assertThat(ExperimentReadiness.evaluate(9_000, 6_500).ready()).isTrue();
    assertThat(ExperimentReadiness.evaluate(9_000, 6_500).remaining()).isZero();
  }

  @Test
  @DisplayName("progress never exceeds one, so a bar cannot overflow")
  void progressIsCapped() {
    assertThat(ExperimentReadiness.evaluate(20_000, 6_500).progress()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("an under powered experiment is refused a winner even when the p-value is tiny")
  void peekingIsRefused() {
    // A real difference, comfortably significant on the numbers alone.
    SignificanceResult significance = ConversionAnalyzer.compare(100, 1000, 160, 1000, 95.0);
    assertThat(significance.significant())
        .as("the raw comparison does clear the threshold")
        .isTrue();

    // But the experiment planned for far more data than it has collected.
    ExperimentReadiness readiness = ExperimentReadiness.evaluate(1_000, 6_500);
    ExperimentAnalysis analysis = new ExperimentAnalysis(significance, readiness);

    assertThat(analysis.canDeclareWinner())
        .as(
            "stopping the moment a p-value dips below the line is exactly what inflates false positives")
        .isFalse();
    assertThat(analysis.summary()).contains("Inconclusive").contains("5500 more");
  }

  @Test
  @DisplayName("a fully powered and significant experiment may declare a winner")
  void poweredAndSignificantCanDeclare() {
    SignificanceResult significance = ConversionAnalyzer.compare(1_300, 6_500, 1_560, 6_500, 95.0);
    ExperimentReadiness readiness = ExperimentReadiness.evaluate(6_500, 6_500);
    ExperimentAnalysis analysis = new ExperimentAnalysis(significance, readiness);

    assertThat(analysis.canDeclareWinner()).isTrue();
    assertThat(analysis.summary()).startsWith("Test wins");
  }

  @Test
  @DisplayName("a fully powered but inconclusive experiment says so rather than declaring")
  void poweredButNotSignificant() {
    SignificanceResult significance = ConversionAnalyzer.compare(1_300, 6_500, 1_310, 6_500, 95.0);
    ExperimentReadiness readiness = ExperimentReadiness.evaluate(6_500, 6_500);
    ExperimentAnalysis analysis = new ExperimentAnalysis(significance, readiness);

    assertThat(analysis.canDeclareWinner()).isFalse();
    assertThat(analysis.summary()).startsWith("No significant difference");
  }
}
