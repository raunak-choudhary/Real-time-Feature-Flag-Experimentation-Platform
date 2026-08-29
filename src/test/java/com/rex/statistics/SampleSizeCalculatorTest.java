package com.rex.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SampleSizeCalculatorTest {

  @Test
  @DisplayName("a 20 percent baseline and 2 point effect needs roughly 6,500 per variant")
  void matchesStandardCalculator() {
    // Standard online calculators give about 6,300 to 6,600 per variant for this setup at
    // 95 percent confidence and 80 percent power, depending on rounding and pooling choices.
    long required = SampleSizeCalculator.requiredPerVariant(0.20, 0.02, 95.0, 80.0);

    assertThat(required).isBetween(6_000L, 7_000L);
  }

  @Test
  @DisplayName("a 5 percent baseline and 1 point effect needs roughly 8,000 per variant")
  void matchesSecondReferencePoint() {
    long required = SampleSizeCalculator.requiredPerVariant(0.05, 0.01, 95.0, 80.0);

    assertThat(required).isBetween(7_000L, 9_500L);
  }

  @Test
  @DisplayName("a smaller detectable effect requires a larger sample")
  void smallerEffectNeedsMoreData() {
    long forTwoPoints = SampleSizeCalculator.requiredPerVariant(0.20, 0.02, 95.0);
    long forOnePoint = SampleSizeCalculator.requiredPerVariant(0.20, 0.01, 95.0);

    assertThat(forOnePoint)
        .as("halving the effect roughly quadruples the sample")
        .isGreaterThan(forTwoPoints * 3);
  }

  @Test
  @DisplayName("higher confidence requires a larger sample")
  void higherConfidenceNeedsMoreData() {
    long atNinetyFive = SampleSizeCalculator.requiredPerVariant(0.20, 0.02, 95.0);
    long atNinetyNine = SampleSizeCalculator.requiredPerVariant(0.20, 0.02, 99.0);

    assertThat(atNinetyNine).isGreaterThan(atNinetyFive);
  }

  @Test
  @DisplayName("higher power requires a larger sample")
  void higherPowerNeedsMoreData() {
    long atEighty = SampleSizeCalculator.requiredPerVariant(0.20, 0.02, 95.0, 80.0);
    long atNinety = SampleSizeCalculator.requiredPerVariant(0.20, 0.02, 95.0, 90.0);

    assertThat(atNinety).isGreaterThan(atEighty);
  }

  @Test
  @DisplayName("impossible parameters are rejected rather than returning a meaningless number")
  void impossibleParametersRejected() {
    assertThatThrownBy(() -> SampleSizeCalculator.requiredPerVariant(0.0, 0.02, 95.0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SampleSizeCalculator.requiredPerVariant(1.0, 0.02, 95.0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> SampleSizeCalculator.requiredPerVariant(0.2, 0.0, 95.0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
