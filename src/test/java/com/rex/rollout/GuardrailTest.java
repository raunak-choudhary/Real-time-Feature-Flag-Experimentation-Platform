package com.rex.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GuardrailTest {

  @ParameterizedTest(name = "observed {0} against threshold {1} {2} is breach={3}")
  @CsvSource({
    "0.03, 0.02, ABOVE, true",
    "0.01, 0.02, ABOVE, false",
    "0.02, 0.02, ABOVE, false",
    "0.01, 0.02, BELOW, true",
    "0.03, 0.02, BELOW, false",
  })
  @DisplayName(
      "breach detection respects the comparison direction and is exclusive at the boundary")
  void breachDetection(
      double observed, double threshold, Guardrail.Comparison comparison, boolean breached) {
    Guardrail guardrail =
        new Guardrail(Guardrail.GuardrailMetric.ERROR_RATE, threshold, comparison);

    assertThat(guardrail.breachedBy(observed)).isEqualTo(breached);
  }

  @Test
  @DisplayName("a conversion guardrail watches for a drop, not a rise")
  void conversionGuardrailWatchesForDrops() {
    Guardrail guardrail =
        new Guardrail(Guardrail.GuardrailMetric.CONVERSION_RATE, 0.10, Guardrail.Comparison.BELOW);

    assertThat(guardrail.breachedBy(0.05)).as("conversion collapsed").isTrue();
    assertThat(guardrail.breachedBy(0.20)).as("conversion improved").isFalse();
  }

  @Test
  @DisplayName("the default minimum observation count is applied when not specified")
  void defaultMinimumObservations() {
    Guardrail guardrail =
        new Guardrail(Guardrail.GuardrailMetric.ERROR_RATE, 0.02, Guardrail.Comparison.ABOVE);

    assertThat(guardrail.minimumObservations()).isEqualTo(Guardrail.DEFAULT_MINIMUM_OBSERVATIONS);
  }

  @Test
  @DisplayName("insufficient data and unavailable both block an advance without being a breach")
  void nonHealthyStatusesBlockAdvance() {
    Guardrail guardrail =
        new Guardrail(Guardrail.GuardrailMetric.ERROR_RATE, 0.02, Guardrail.Comparison.ABOVE);

    assertThat(
            new GuardrailVerdict(guardrail, 0.0, 5, GuardrailVerdict.Status.INSUFFICIENT_DATA)
                .blocksAdvance())
        .isTrue();
    assertThat(
            new GuardrailVerdict(guardrail, 0.0, 500, GuardrailVerdict.Status.UNAVAILABLE)
                .blocksAdvance())
        .as("a monitoring outage must not silently become an unguarded rollout")
        .isTrue();
    assertThat(
            new GuardrailVerdict(guardrail, 0.001, 500, GuardrailVerdict.Status.HEALTHY)
                .blocksAdvance())
        .isFalse();
  }

  @Test
  @DisplayName("a breach describes itself with the numbers that caused it")
  void breachDescribesItself() {
    Guardrail guardrail =
        new Guardrail(Guardrail.GuardrailMetric.ERROR_RATE, 0.02, Guardrail.Comparison.ABOVE);
    GuardrailVerdict verdict =
        new GuardrailVerdict(guardrail, 0.085, 1_200, GuardrailVerdict.Status.BREACHED);

    assertThat(verdict.describe())
        .contains("ERROR_RATE")
        .contains("0.0850")
        .contains("0.0200")
        .contains("1200");
  }
}
