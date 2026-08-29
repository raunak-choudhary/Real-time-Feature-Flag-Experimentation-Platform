package com.rex.statistics;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Checked against standard normal tables, so the approximations are verified independently. */
class NormalDistributionTest {

  @ParameterizedTest(name = "phi({0}) = {1}")
  @CsvSource({
    "0.00, 0.50000",
    "1.00, 0.84134",
    "1.645, 0.95000",
    "1.96, 0.97500",
    "2.00, 0.97725",
    "2.576, 0.99500",
    "3.00, 0.99865",
    "-1.96, 0.02500",
  })
  @DisplayName("the CDF matches published normal tables to four decimal places")
  void cdfMatchesTables(double x, double expected) {
    assertThat(NormalDistribution.cdf(x)).isCloseTo(expected, Offset.offset(0.0001));
  }

  @ParameterizedTest(name = "critical value at {0} percent is {1}")
  @CsvSource({"90.0, 1.6449", "95.0, 1.9600", "99.0, 2.5758"})
  @DisplayName("critical values match the textbook figures")
  void criticalValuesMatchTables(double confidence, double expected) {
    assertThat(NormalDistribution.criticalValue(confidence))
        .isCloseTo(expected, Offset.offset(0.001));
  }

  @Test
  @DisplayName("the CDF is symmetric about zero")
  void cdfIsSymmetric() {
    for (double x = 0.1; x < 4.0; x += 0.1) {
      assertThat(NormalDistribution.cdf(x) + NormalDistribution.cdf(-x))
          .isCloseTo(1.0, Offset.offset(0.0001));
    }
  }

  @Test
  @DisplayName("the inverse CDF undoes the CDF")
  void inverseIsConsistentWithCdf() {
    for (double p = 0.01; p < 0.99; p += 0.01) {
      double x = NormalDistribution.inverseCdf(p);
      assertThat(NormalDistribution.cdf(x)).isCloseTo(p, Offset.offset(0.0001));
    }
  }
}
