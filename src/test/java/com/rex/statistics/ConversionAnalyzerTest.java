package com.rex.statistics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verified against worked examples rather than against the implementation itself.
 *
 * <p>A statistics test that only checks self consistency proves the code does what it does, which
 * is worth nothing. Each expected value below is derived by hand from the standard formulae.
 */
class ConversionAnalyzerTest {

  private static final double CONFIDENCE = 95.0;

  @Test
  @DisplayName("worked example: 200/1000 against 240/1000 reproduces z = 2.159 and p = 0.0308")
  void reproducesWorkedExample() {
    // pooled p = 440/2000 = 0.22
    // SE = sqrt(0.22 * 0.78 * (1/1000 + 1/1000)) = 0.0185256
    // z  = (0.24 - 0.20) / 0.0185256 = 2.15916
    // p  = 2 * (1 - phi(2.15916)) = 0.03084
    SignificanceResult result = ConversionAnalyzer.compare(200, 1000, 240, 1000, CONFIDENCE);

    assertThat(result.zScore()).isCloseTo(2.1592, Offset.offset(0.001));
    assertThat(result.pValue()).isCloseTo(0.0308, Offset.offset(0.001));
    assertThat(result.significant()).isTrue();
    assertThat(result.verdict()).isEqualTo(SignificanceResult.Verdict.TEST_WINS);
  }

  @Test
  @DisplayName("identical rates produce a z of zero and a p-value of one")
  void identicalRatesAreNotSignificant() {
    SignificanceResult result = ConversionAnalyzer.compare(100, 1000, 100, 1000, CONFIDENCE);

    assertThat(result.zScore()).isCloseTo(0.0, Offset.offset(1e-9));
    assertThat(result.pValue()).isCloseTo(1.0, Offset.offset(1e-6));
    assertThat(result.significant()).isFalse();
    assertThat(result.verdict()).isEqualTo(SignificanceResult.Verdict.NO_SIGNIFICANT_DIFFERENCE);
  }

  @Test
  @DisplayName("a control that beats the test is reported as control winning, not as no difference")
  void controlCanWin() {
    SignificanceResult result = ConversionAnalyzer.compare(240, 1000, 200, 1000, CONFIDENCE);

    assertThat(result.zScore()).isNegative();
    assertThat(result.significant()).isTrue();
    assertThat(result.verdict()).isEqualTo(SignificanceResult.Verdict.CONTROL_WINS);
  }

  @Test
  @DisplayName(
      "lift is reported both absolutely and relatively, since 1 point off 2 is not 1 off 50")
  void reportsBothLifts() {
    SignificanceResult result = ConversionAnalyzer.compare(200, 1000, 240, 1000, CONFIDENCE);

    assertThat(result.absoluteLift()).isCloseTo(0.04, Offset.offset(1e-9));
    assertThat(result.relativeLift()).isCloseTo(0.20, Offset.offset(1e-9));
  }

  @Test
  @DisplayName("a tiny sample reports insufficient data rather than a p-value")
  void tinySampleIsInsufficient() {
    SignificanceResult result = ConversionAnalyzer.compare(1, 10, 5, 10, CONFIDENCE);

    assertThat(result.verdict()).isEqualTo(SignificanceResult.Verdict.INSUFFICIENT_DATA);
    assertThat(result.significant())
        .as("a 5x difference on ten users must never read as significant")
        .isFalse();
  }

  @Test
  @DisplayName("zero exposures returns insufficient data rather than dividing by zero")
  void zeroExposuresDoesNotDivideByZero() {
    SignificanceResult result = ConversionAnalyzer.compare(0, 0, 0, 0, CONFIDENCE);

    assertThat(result.verdict()).isEqualTo(SignificanceResult.Verdict.INSUFFICIENT_DATA);
    assertThat(result.controlRate()).isZero();
  }

  @Test
  @DisplayName("zero conversions on both sides is not significant and does not produce NaN")
  void zeroConversionsIsStable() {
    SignificanceResult result = ConversionAnalyzer.compare(0, 5000, 0, 5000, CONFIDENCE);

    assertThat(result.zScore()).isNotNaN().isZero();
    assertThat(result.pValue()).isNotNaN();
    assertThat(result.significant()).isFalse();
  }

  @Test
  @DisplayName("a very large equal sample stays stable rather than underflowing")
  void largeEqualSamplesAreStable() {
    SignificanceResult result =
        ConversionAnalyzer.compare(2_000_000, 10_000_000, 2_000_000, 10_000_000, CONFIDENCE);

    assertThat(result.pValue()).isNotNaN().isCloseTo(1.0, Offset.offset(1e-6));
    assertThat(result.significant()).isFalse();
  }

  @Test
  @DisplayName("a stricter confidence level makes significance harder to reach")
  void higherConfidenceIsStricter() {
    SignificanceResult atNinetyFive = ConversionAnalyzer.compare(200, 1000, 240, 1000, 95.0);
    SignificanceResult atNinetyNine = ConversionAnalyzer.compare(200, 1000, 240, 1000, 99.0);

    assertThat(atNinetyFive.significant()).isTrue();
    assertThat(atNinetyNine.significant())
        .as("p = 0.0308 clears a 5 percent threshold but not a 1 percent one")
        .isFalse();
  }

  @Test
  @DisplayName("conversions exceeding exposures is rejected as impossible input")
  void impossibleInputIsRejected() {
    assertThatThrownBy(() -> ConversionAnalyzer.compare(1001, 1000, 100, 1000, CONFIDENCE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot exceed exposures");

    assertThatThrownBy(() -> ConversionAnalyzer.compare(-1, 1000, 100, 1000, CONFIDENCE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("both variants carry a confidence interval alongside the verdict")
  void intervalsAccompanyTheVerdict() {
    SignificanceResult result = ConversionAnalyzer.compare(200, 1000, 240, 1000, CONFIDENCE);

    assertThat(result.controlInterval()).isNotNull();
    assertThat(result.testInterval()).isNotNull();
    assertThat(result.controlInterval().lower()).isLessThan(result.controlRate());
    assertThat(result.controlInterval().upper()).isGreaterThan(result.controlRate());
  }
}
