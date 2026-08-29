package com.rex.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class WilsonIntervalTest {

  private static final double CONFIDENCE = 95.0;

  @Test
  @DisplayName("worked example: 200/1000 at 95 percent gives roughly 0.1764 to 0.2259")
  void reproducesWorkedExample() {
    // centre = (0.2 + 1.96^2/2000) / (1 + 1.96^2/1000) = 0.201148
    // margin = (1.96 / 1.0038416) * sqrt(0.2*0.8/1000 + 1.96^2/(4*10^6)) = 0.024772
    ConfidenceInterval interval = WilsonInterval.forProportion(200, 1000, CONFIDENCE);

    assertThat(interval.lower()).isCloseTo(0.1764, Offset.offset(0.001));
    assertThat(interval.upper()).isCloseTo(0.2259, Offset.offset(0.001));
  }

  @Test
  @DisplayName("zero conversions gives a lower bound of zero, not a negative rate")
  void zeroConversionsStaysAtOrAboveZero() {
    ConfidenceInterval interval = WilsonInterval.forProportion(0, 100, CONFIDENCE);

    assertThat(interval.lower())
        .as("the normal approximation would go negative here, which is not a possible rate")
        .isGreaterThanOrEqualTo(0.0);
    assertThat(interval.upper())
        .as("no conversions in 100 trials is evidence of a low rate, not of certainty")
        .isGreaterThan(0.0);
  }

  @Test
  @DisplayName("every conversion gives an upper bound of one, not above it")
  void allConversionsStaysAtOrBelowOne() {
    ConfidenceInterval interval = WilsonInterval.forProportion(100, 100, CONFIDENCE);

    assertThat(interval.upper()).isLessThanOrEqualTo(1.0);
    assertThat(interval.lower()).isLessThan(1.0);
  }

  @ParameterizedTest(name = "{0}/{1} produces bounds inside 0 to 1")
  @CsvSource({"0,10", "1,10", "5,10", "9,10", "10,10", "1,1000000", "999999,1000000"})
  @DisplayName("bounds never leave the range a proportion can occupy")
  void boundsAreAlwaysValid(long successes, long trials) {
    ConfidenceInterval interval = WilsonInterval.forProportion(successes, trials, CONFIDENCE);

    assertThat(interval.lower()).isBetween(0.0, 1.0);
    assertThat(interval.upper()).isBetween(0.0, 1.0);
    assertThat(interval.lower()).isLessThanOrEqualTo(interval.upper());
  }

  @Test
  @DisplayName("a larger sample narrows the interval")
  void moreDataNarrowsTheInterval() {
    ConfidenceInterval small = WilsonInterval.forProportion(20, 100, CONFIDENCE);
    ConfidenceInterval large = WilsonInterval.forProportion(2000, 10_000, CONFIDENCE);

    assertThat(large.width()).isLessThan(small.width());
  }

  @Test
  @DisplayName("a higher confidence level widens the interval")
  void moreConfidenceWidensTheInterval() {
    ConfidenceInterval ninetyFive = WilsonInterval.forProportion(200, 1000, 95.0);
    ConfidenceInterval ninetyNine = WilsonInterval.forProportion(200, 1000, 99.0);

    assertThat(ninetyNine.width()).isGreaterThan(ninetyFive.width());
  }

  @Test
  @DisplayName("excludes reports whether a value falls outside the interval")
  void excludesDetectsValuesOutsideTheBounds() {
    ConfidenceInterval interval = WilsonInterval.forProportion(200, 1000, CONFIDENCE);

    assertThat(interval.excludes(0.0)).isTrue();
    assertThat(interval.excludes(0.20)).isFalse();
  }

  @Test
  @DisplayName("impossible inputs are rejected")
  void impossibleInputsRejected() {
    assertThatThrownBy(() -> WilsonInterval.forProportion(5, 0, CONFIDENCE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("trials must be positive");

    assertThatThrownBy(() -> WilsonInterval.forProportion(11, 10, CONFIDENCE))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
