package com.rex.statistics;

/** A bounded estimate of a proportion. */
public record ConfidenceInterval(double lower, double upper, double confidenceLevel) {

  public double width() {
    return upper - lower;
  }

  /** Whether the interval excludes a value, which is how a difference is judged real. */
  public boolean excludes(double value) {
    return value < lower || value > upper;
  }
}
